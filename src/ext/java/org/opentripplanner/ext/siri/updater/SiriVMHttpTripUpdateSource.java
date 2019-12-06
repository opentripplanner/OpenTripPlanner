package org.opentripplanner.ext.siri.updater;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.ext.siri.SiriHttpUtils;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.JsonConfigurable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.Siri;

import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SiriVMHttpTripUpdateSource implements VehicleMonitoringSource, JsonConfigurable {
    private static final Logger LOG =
            LoggerFactory.getLogger(SiriVMHttpTripUpdateSource.class);

    /**
     * True iff the last list with updates represent all updates that are active right now, i.e. all
     * previous updates should be disregarded
     */
    private boolean fullDataset = true;

    /**
     * Feed id that is used to match trip ids in the TripUpdates
     */
    private String feedId;

    private String url;

    private ZonedDateTime lastTimestamp = ZonedDateTime.now().minusMonths(1);

    private String requestorRef;
    private int timeout;

    private static Map<String, String> requestHeaders = new HashMap<>();

    @Override
    public void configure(Graph graph, JsonNode config) throws Exception {
        String url = config.path("url").asText();
        if (url == null) {
            throw new IllegalArgumentException("Missing mandatory 'url' parameter");
        }
        this.url = url;

        this.requestorRef = config.path("requestorRef").asText();
        if (requestorRef == null || requestorRef.isEmpty()) {
            requestorRef = "otp-"+UUID.randomUUID().toString();
        }
        this.feedId = config.path("feedId").asText();

        int timeoutSec = config.path("timeoutSec").asInt();
        if (timeoutSec > 0) {
            this.timeout = 1000*timeoutSec;
        }

        requestHeaders.put("ET-Client-Name", SiriHttpUtils.getUniqueETClientName("-VM"));
    }

    @Override
    public Siri getUpdates() {
        long t1 = System.currentTimeMillis();
        long creating = 0;
        long fetching = 0;
        long unmarshalling = 0;

        fullDataset = false;
        try {
            String vmServiceRequest = SiriHelper.createVMServiceRequestAsXml(requestorRef);
            creating = System.currentTimeMillis()-t1;
            t1 = System.currentTimeMillis();

            InputStream is = SiriHttpUtils.postData(url, vmServiceRequest, timeout, requestHeaders);
            if (is != null) {
                // Decode message
                fetching = System.currentTimeMillis()-t1;
                t1 = System.currentTimeMillis();
                Siri siri = SiriHelper.unmarshal(is);
                unmarshalling = System.currentTimeMillis()-t1;

                if (siri.getServiceDelivery().getResponseTimestamp().isBefore(lastTimestamp)) {
                    LOG.info("Newer data has already been processed");
                    return null;
                }
                lastTimestamp = siri.getServiceDelivery().getResponseTimestamp();

                return siri;

            }
        } catch (Exception e) {
            LOG.info("Failed after {} ms", (System.currentTimeMillis()-t1));
            LOG.warn("Failed to parse SIRI-VM feed from " + url + ":", e);
        } finally {
            LOG.info("Updating VM [{}]: Create req: {}, Fetching data: {}, Unmarshalling: {}", requestorRef, creating, fetching, unmarshalling);
        }
        return null;
    }

    @Override
    public boolean getFullDatasetValueOfLastUpdates() {
        return fullDataset;
    }

    public String toString() {
        return "SiriVMHttpTripUpdateSource(" + url + ")";
    }

    @Override
    public String getFeedId() {
        return this.feedId;
    }
}
