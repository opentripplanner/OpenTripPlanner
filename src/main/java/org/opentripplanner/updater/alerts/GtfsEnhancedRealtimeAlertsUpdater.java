package org.opentripplanner.updater.alerts;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.transit.realtime.GtfsRealtime;
import org.apache.commons.io.IOUtils;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.AlertPatchServiceImpl;
import org.opentripplanner.routing.services.AlertPatchService;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.GtfsRealtimeFuzzyTripMatcher;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.transit.realtime.GtfsRealtime.FeedMessage;


public class GtfsEnhancedRealtimeAlertsUpdater extends PollingGraphUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(GtfsEnhancedRealtimeAlertsUpdater.class);

    private GraphUpdaterManager updaterManager;

    private Long lastTimestamp = Long.MIN_VALUE;

    private String url;

    private String feedId;

    private GtfsRealtimeFuzzyTripMatcher fuzzyTripMatcher;

    private AlertPatchService alertPatchService;

    private long earlyStart;

    private AlertsUpdateHandler updateHandler = null;

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
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
        this.url = url;
        this.earlyStart = config.path("earlyStartSec").asInt(0);
        this.feedId = config.path("feedId").asText();
        if (config.path("fuzzyTripMatching").asBoolean(false)) {
            this.fuzzyTripMatcher = new GtfsRealtimeFuzzyTripMatcher(graph.index);
        }
        LOG.info("Creating enhanced real-time alert (json) updater running every {} seconds : {}", pollingPeriodSeconds, url);
    }

    @Override
    public void setup(Graph graph) {
        if (updateHandler == null) {
            updateHandler = new AlertsUpdateHandler();
        }
        updateHandler.setEarlyStart(earlyStart);
        updateHandler.setFeedId(feedId);
        updateHandler.setAlertPatchService(alertPatchService);
        updateHandler.setFuzzyTripMatcher(fuzzyTripMatcher);
    }

    @Override
    protected void runPolling() {
        try {
            InputStream data = HttpUtils.getData(url);
            if (data == null) {
                throw new RuntimeException("Failed to get json data from url " + url);
            }

            String stringData = IOUtils.toString(data);
            LOG.info("Read enhanced json stream from " + url + " : " + stringData);

            final FeedMessage feed = parseJson(stringData);

            long feedTimestamp = feed.getHeader().getTimestamp();
            if (feedTimestamp <= lastTimestamp) {
                LOG.info("Ignoring feed with an old timestamp.");
                return;
            }

            // Handle update in graph writer runnable
            updaterManager.execute(new GraphWriterRunnable() {
                @Override
                public void run(Graph graph) {
                    updateHandler.update(feed);
                }
            });

            LOG.info(feed.getEntityCount() + " alerts parsed");

            lastTimestamp = feedTimestamp;
        } catch (Exception e) {
            LOG.error("Error reading enhanced gtfs-realtime json feed from " + url, e);
        }
    }

    private FeedMessage parseJson(String data) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(data);

        JsonNode headerNode = json.get("header");
        GtfsRealtime.FeedHeader header = GtfsRealtime.FeedHeader
                .newBuilder()
                .setTimestamp(headerNode.get("timestamp").longValue())
                .setGtfsRealtimeVersion(headerNode.get("gtfs_realtime_version").textValue())
                .build();

        FeedMessage.Builder result = FeedMessage
                .newBuilder()
                .setHeader(header);

        for (JsonNode entity: json.get("entity")) {
            JsonNode alert = entity.get("alert");

            if (alert == null) {
                continue;
            }

            result.addEntity(parseEntity(entity));
        }

        return result.build();
    }

    private GtfsRealtime.FeedEntity parseEntity(JsonNode entity) {
        JsonNode alertNode = entity.get("alert");

        GtfsRealtime.Alert.Builder alert = GtfsRealtime.Alert
                .newBuilder()
                .setDescriptionText(parseTranslatedString(alertNode.get("description_text")));

        if (alertNode.get("effect") != null) {
            alert.setEffect(GtfsRealtime.Alert.Effect.valueOf(alertNode.get("effect").textValue()));
        }

        if (alertNode.get("url") != null) {
            alert.setUrl(parseTranslatedString(alertNode.get("url")));
        }

        if (alertNode.get("active_period") != null) {
            for (JsonNode period : alertNode.get("active_period")) {
                alert.addActivePeriod(parsePeriod(period));
            }
        }

        if (alertNode.get("informed_entity") != null) {
            for (JsonNode ie : alertNode.get("informed_entity")) {
                alert.addInformedEntity(parseInformedEntity(ie));
            }
        }

        return GtfsRealtime.FeedEntity
                .newBuilder()
                .setId(entity.get("id").textValue())
                .setAlert(alert.build())
                .build();
    }

    private GtfsRealtime.EntitySelector parseInformedEntity(JsonNode ie) {
        GtfsRealtime.EntitySelector.Builder builder = GtfsRealtime.EntitySelector
                .newBuilder();

        if (ie.get("agency_id") != null) {
            builder.setAgencyId(ie.get("agency_id").textValue());
        }

        if (ie.get("route_type") != null) {
            builder.setRouteType(ie.get("route_type").intValue());
        }

        if (ie.get("route_id") != null) {
            builder.setRouteId(ie.get("route_id").textValue());
        }

        if (ie.get("stop_id") != null) {
            builder.setStopId(ie.get("stop_id").textValue());
        }

        JsonNode tripNode = ie.get("trip");
        if (tripNode != null) {
            GtfsRealtime.TripDescriptor.Builder tripBuilder = GtfsRealtime.TripDescriptor
                    .newBuilder()
                    .setTripId(tripNode.get("trip_id").textValue())
                    .setRouteId(tripNode.get("route_id").textValue());

            if (tripNode.get("direction_id") != null) {
                tripBuilder.setDirectionId(tripNode.get("direction_id").intValue());
            }

            builder.setTrip(tripBuilder.build());
        }

        List<String> activities = new ArrayList<>();
        for (JsonNode activity: ie.get("activities")) {
            activities.add(activity.textValue());
        }
        builder.setActivities(activities);

        return builder.build();
    }

    private GtfsRealtime.TimeRange parsePeriod(JsonNode period) {
        GtfsRealtime.TimeRange.Builder builder = GtfsRealtime.TimeRange
                .newBuilder()
                .setStart(period.get("start").longValue());

        if (period.get("end") != null) {
            builder.setEnd(period.get("end").longValue());
        }

        return builder.build();
    }

    private GtfsRealtime.TranslatedString parseTranslatedString(JsonNode node) {
        GtfsRealtime.TranslatedString.Builder builder = GtfsRealtime.TranslatedString.newBuilder();

        for (JsonNode translation: node.get("translation")) {
            builder.addTranslation(GtfsRealtime.TranslatedString.Translation
                    .newBuilder()
                    .setLanguage(translation.get("language").textValue())
                    .setText(translation.get("text").textValue())
                    .build());
        }

        return builder.build();
    }

    @Override
    public void teardown() {
    }

    public String toString() {
        return "GtfsEnhancedRealtimeAlertsUpdater(" + url + ")";
    }
}
