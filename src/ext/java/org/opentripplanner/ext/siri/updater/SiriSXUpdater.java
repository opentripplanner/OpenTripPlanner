package org.opentripplanner.ext.siri.updater;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.BooleanUtils;
import org.opentripplanner.ext.siri.SiriAlertsUpdateHandler;
import org.opentripplanner.ext.siri.SiriFuzzyTripMatcher;
import org.opentripplanner.ext.siri.SiriHttpUtils;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.opentripplanner.updater.WriteToGraphCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.ServiceDelivery;
import uk.org.siri.siri20.Siri;

public class SiriSXUpdater extends PollingGraphUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(SiriSXUpdater.class);
    private static final long RETRY_INTERVAL_MILLIS = 5000;

    private WriteToGraphCallback saveResultOnGraph;

    private ZonedDateTime lastTimestamp = ZonedDateTime.now().minusWeeks(1);

    private final String url;

    private final String feedId;

    private TransitAlertService transitAlertService;

    private final long earlyStart;

    private SiriAlertsUpdateHandler updateHandler = null;

    private String requestorRef;

    private int timeout;

    private static final Map<String, String> requestHeaders = new HashMap<>();


    private int retryCount = 0;
    private final String originalRequestorRef;

    public SiriSXUpdater(SiriSXUpdaterParameters config) {
        super(config);
        // TODO: add options to choose different patch services
        this.url = config.getUrl();
        this.requestorRef = config.getRequestorRef();
        this.earlyStart = config.getEarlyStartSec();
        this.feedId = config.getFeedId();

        if (requestorRef == null || requestorRef.isEmpty()) {
            requestorRef = "otp-" + UUID.randomUUID().toString();
        }

        //Keeping original requestorRef use as base for updated requestorRef to be used in retries
        this.originalRequestorRef = requestorRef;

        int timeoutSec = config.getTimeoutSec();
        if (timeoutSec > 0) {
            this.timeout = 1000 * timeoutSec;
        }

        blockReadinessUntilInitialized = config.blockReadinessUntilInitialized();

        requestHeaders.put("ET-Client-Name", SiriHttpUtils.getUniqueETClientName("-SX"));

        LOG.info("Creating real-time alert updater (SIRI SX) running every {} seconds : {}", pollingPeriodSeconds, url);
    }

    @Override
    public void setGraphUpdaterManager(WriteToGraphCallback saveResultOnGraph) {
        this.saveResultOnGraph = saveResultOnGraph;
    }

    @Override
    public void setup(Graph graph) {
        this.transitAlertService = new TransitAlertServiceImpl(graph);
        SiriFuzzyTripMatcher fuzzyTripMatcher = new SiriFuzzyTripMatcher(new RoutingService(graph));
        if (updateHandler == null) {
            updateHandler = new SiriAlertsUpdateHandler(feedId, graph);
        }
        updateHandler.setEarlyStart(earlyStart);
        updateHandler.setTransitAlertService(transitAlertService);
        updateHandler.setSiriFuzzyTripMatcher(fuzzyTripMatcher);

    }

    @Override
    protected void runPolling() {
        try {
            boolean moreData = false;
            do {
                Siri updates = getUpdates();
                if (updates != null) {
                    ServiceDelivery serviceDelivery = updates.getServiceDelivery();
                    // Use isTrue in case isMoreData returns null. Mark the updater as primed after last page of updates.
                    moreData = BooleanUtils.isTrue(serviceDelivery.isMoreData());
                    final boolean markPrimed = !moreData;
                    if (serviceDelivery.getSituationExchangeDeliveries() != null) {
                        saveResultOnGraph.execute(graph -> {
                            updateHandler.update(serviceDelivery);
                            if (markPrimed) primed = true;
                        });
                    }
                }
            } while (moreData);
        } catch (IOException e) {

            final long sleepTime = RETRY_INTERVAL_MILLIS + RETRY_INTERVAL_MILLIS * retryCount;

            retryCount++;

            LOG.info("Caught timeout - retry no. {} after {} millis", retryCount, sleepTime);

            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException ex) {
                //Ignore
            }

            // Creating new requestorRef so all data is refreshed
            requestorRef = originalRequestorRef + "-retry-" + retryCount;
            runPolling();
        }
    }

    private Siri getUpdates() throws IOException {

        long t1 = System.currentTimeMillis();
        long creating = 0;
        long fetching = 0;
        long unmarshalling = 0;
        try {
            String sxServiceRequest = SiriHelper.createSXServiceRequestAsXml(requestorRef);
            creating = System.currentTimeMillis()-t1;
            t1 = System.currentTimeMillis();

            InputStream is = SiriHttpUtils.postData(url, sxServiceRequest, timeout, requestHeaders);

            fetching = System.currentTimeMillis()-t1;
            t1 = System.currentTimeMillis();

            Siri siri = SiriHelper.unmarshal(is);

            unmarshalling = System.currentTimeMillis()-t1;
            if (siri == null) {
                throw new RuntimeException("Failed to get data from url " + url);
            }
            ServiceDelivery serviceDelivery = siri.getServiceDelivery();
            if (serviceDelivery == null) {
                throw new RuntimeException("Failed to get serviceDelivery " + url);
            }

            ZonedDateTime responseTimestamp = serviceDelivery.getResponseTimestamp();
            if (responseTimestamp.isBefore(lastTimestamp)) {
                LOG.info("Ignoring feed with an old timestamp.");
                return null;
            }

            lastTimestamp = responseTimestamp;
            return siri;
        } catch (IOException e) {
            LOG.info("Failed after {} ms", (System.currentTimeMillis()-t1));
            LOG.error("Error reading SIRI feed from " + url, e);
            throw e;
        } catch (Exception e) {
            LOG.info("Failed after {} ms", (System.currentTimeMillis()-t1));
            LOG.error("Error reading SIRI feed from " + url, e);
        } finally {
            LOG.info("Updating SX [{}]: Create req: {}, Fetching data: {}, Unmarshalling: {}", requestorRef, creating, fetching, unmarshalling);
        }
        return null;
    }

    @Override
    public void teardown() {
    }

    public TransitAlertService getTransitAlertService() {
        return transitAlertService;
    }

    public String toString() {
        return "SiriSXUpdater (" + url + ")";
    }

}
