package org.opentripplanner.ext.siri.updater;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.BooleanUtils;
import org.opentripplanner.ext.siri.SiriAlertsUpdateHandler;
import org.opentripplanner.ext.siri.SiriFuzzyTripMatcher;
import org.opentripplanner.ext.siri.SiriHttpUtils;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.AlertPatchServiceImpl;
import org.opentripplanner.routing.services.AlertPatchService;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.ServiceDelivery;
import uk.org.siri.siri20.Siri;

import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SiriSXUpdater extends PollingGraphUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(SiriSXUpdater.class);

    private GraphUpdaterManager updaterManager;

    private ZonedDateTime lastTimestamp = ZonedDateTime.now().minusWeeks(1);

    private String url;

    private String feedId;

    private SiriFuzzyTripMatcher fuzzyTripMatcher;

    private AlertPatchService alertPatchService;

    private long earlyStart;

    private SiriAlertsUpdateHandler updateHandler = null;

    private String requestorRef;

    private int timeout;

    private static Map<String, String> requestHeaders = new HashMap<>();


    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    @Override
    public void setup(Graph graph) throws Exception {
        if (updateHandler == null) {
            updateHandler = new SiriAlertsUpdateHandler(feedId);
        }
        updateHandler.setEarlyStart(earlyStart);
        updateHandler.setAlertPatchService(alertPatchService);
        updateHandler.setSiriFuzzyTripMatcher(fuzzyTripMatcher);

    }

    @Override
    protected void configurePolling(Graph graph, JsonNode config) throws Exception {
        // TODO: add options to choose different patch services
        AlertPatchService alertPatchService = new AlertPatchServiceImpl(graph);
        this.alertPatchService = alertPatchService;
        String url = config.path("url").asText();
        if (url == null) {
            throw new IllegalArgumentException("Missing mandatory 'url' parameter");
        }

        this.requestorRef = config.path("requestorRef").asText();
        if (requestorRef == null || requestorRef.isEmpty()) {
            requestorRef = "otp-"+UUID.randomUUID().toString();
        }

        this.url = url;// + uniquenessParameter;
        this.earlyStart = config.path("earlyStartSec").asInt(0);
        this.feedId = config.path("feedId").asText();


        int timeoutSec = config.path("timeoutSec").asInt();
        if (timeoutSec > 0) {
            this.timeout = 1000*timeoutSec;
        }

        blockReadinessUntilInitialized = config.path("blockReadinessUntilInitialized").asBoolean(false);

        this.fuzzyTripMatcher = new SiriFuzzyTripMatcher(new RoutingService(graph));

        requestHeaders.put("ET-Client-Name", SiriHttpUtils.getUniqueETClientName("-SX"));

        LOG.info("Creating real-time alert updater (SIRI SX) running every {} seconds : {}", pollingPeriodSeconds, url);
    }

    @Override
    protected void runPolling() throws Exception {
        boolean moreData = false;
        do {
            Siri updates = getUpdates();
            if (updates != null) {
                ServiceDelivery serviceDelivery = updates.getServiceDelivery();
                // Use isTrue in case isMoreData returns null. Mark the updater as primed after last page of updates.
                moreData = BooleanUtils.isTrue(serviceDelivery.isMoreData());
                final boolean markPrimed = !moreData;
                if (serviceDelivery.getSituationExchangeDeliveries() != null) {
                    updaterManager.execute(graph -> {
                        updateHandler.update(serviceDelivery);
                        if (markPrimed) primed = true;
                    });
                }
            }
        } while (moreData);
    }

    private Siri getUpdates() {

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

    public AlertPatchService getAlertPatchService() {
        return alertPatchService;
    }

    public String toString() {
        return "SiriSXUpdater (" + url + ")";
    }
}
