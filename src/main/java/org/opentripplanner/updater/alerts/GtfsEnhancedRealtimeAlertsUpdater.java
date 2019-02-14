package org.opentripplanner.updater.alerts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.EnhancedAlert;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GtfsEnhancedRealtimeAlertsUpdater extends PollingGraphUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(GtfsEnhancedRealtimeAlertsUpdater.class);

    private GraphUpdaterManager updaterManager;

    private String url;

    private Map<String, List<EnhancedAlert>> alertDetails;

    private final String AGENCY_ID = "1";

    @Override
    protected void runPolling() {
        try {
            InputStream data = HttpUtils.getData(url);
            if (data == null) {
                throw new RuntimeException("Failed to get data from url " + url);
            }

            alertDetails = parseJson(IOUtils.toString(data));
            updaterManager.execute(this::updateGraph);
        } catch (Exception e) {
            LOG.error("Error reading enhanced feed from " + url, e);
        }
    }

    @Override
    protected void configurePolling(Graph graph, JsonNode config) {
        String url = config.path("url").asText();
        if (url == null) {
            throw new IllegalArgumentException("Missing mandatory 'url' parameter");
        }
        this.url = url;
        LOG.info("Creating enhanced alert feed updater running every {} seconds: {}", pollingPeriodSeconds, url);
    }

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    @Override
    public void setup(Graph graph) {}

    @Override
    public void teardown() {}

    public String toString() {
        return "GtfsEnhancedRealtimeAlertsUpdater(" + url + ")";
    }

    private Map<String, List<EnhancedAlert>> parseJson(String jsonString) throws Exception {
        Map<String, List<EnhancedAlert>> result = new HashMap<>();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(jsonString);

        for (JsonNode node: json.get("entity")) {
            JsonNode alert = node.get("alert");
            if (alert == null) {
                continue;
            }


            JsonNode informedEntity = alert.get("informed_entity");
            if (informedEntity == null) {
                continue;
            }

            String id = node.get("id").textValue();

            List<EnhancedAlert> enhancedAlerts = new ArrayList<>();

            for (JsonNode ie: informedEntity) {
                EnhancedAlert enhancedAlert = new EnhancedAlert();

                if (ie.hasNonNull("route_id")) {
                    enhancedAlert.setRoute(new FeedScopedId(AGENCY_ID, ie.get("route_id").textValue()));
                }
                if (ie.hasNonNull("stop_id")) {
                    enhancedAlert.setStop(new FeedScopedId(AGENCY_ID, ie.get("stop_id").textValue()));
                }
                if (ie.hasNonNull("trip") && ie.get("trip").hasNonNull("trip_id")) {
                    enhancedAlert.setTrip(new FeedScopedId(AGENCY_ID, ie.get("trip").get("trip_id").textValue()));
                }

                List<EnhancedAlert.AffectedActivity> activities = new ArrayList<>();

                for (JsonNode activity: ie.get("activities")) {
                    activities.add(EnhancedAlert.AffectedActivity.valueOf(activity.textValue()));
                }

                enhancedAlert.setActivities(activities);

                enhancedAlerts.add(enhancedAlert);
            }

            result.put(id, enhancedAlerts);
        }

        return result;
    }

    private void updateGraph(Graph graph) {
        for (AlertPatch alertPatch: graph.getAllAlertPatches()) {
            List<EnhancedAlert> newAlerts = new ArrayList<>();

            List<EnhancedAlert> enhancedAlerts = alertDetails.get(alertPatch.getAlert().getId());
            for (EnhancedAlert alert: enhancedAlerts) {
                if (alert.appliesTo(alertPatch)) {
                    newAlerts.add(alert);
                }
            }

            alertPatch.setEnhancedAlerts(newAlerts);
        }
    }
}
