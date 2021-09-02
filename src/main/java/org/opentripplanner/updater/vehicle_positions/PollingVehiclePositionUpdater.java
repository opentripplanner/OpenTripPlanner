package org.opentripplanner.updater.vehicle_positions;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GtfsRealtimeFuzzyTripMatcher;
import org.opentripplanner.updater.JsonConfigurable;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Add vehicle positions to OTP patterns via a realtime source
 **
 * <pre>
 * rt.type = vehicle-position-updater
 * rt.frequencySec = 60
 * rt.sourceType = gtfs-http
 * rt.url = http://host.tld/path
 * rt.feedId = TA
 * </pre>
 *
 */
public class PollingVehiclePositionUpdater extends PollingGraphUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(PollingVehiclePositionUpdater.class);

    /**
     * Parent update manager. Is used to execute graph writer runnables.
     */
    private GraphUpdaterManager updaterManager;

    /**
     * Update streamer
     */
    private VehiclePositionSource vehiclePositionSource;

    /**
     * Feed id that is used for the trip ids in the TripUpdates
     */
    private String feedId;


    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    @Override
    public void configurePolling(Graph graph, JsonNode config) throws Exception {
        // Create update streamer from preferences
        feedId = config.path("feedId").asText("");
        String sourceType = config.path("sourceType").asText();
        if (sourceType != null) {
            if (sourceType.equals("gtfs-http")) {
                vehiclePositionSource = new GtfsRealtimeHttpVehiclePositionSource();
            } else if (sourceType.equals("gtfs-file")) {
                vehiclePositionSource = new GtfsRealtimeFileVehiclePositionSource();
            }
        }

        // Configure update source
        if (vehiclePositionSource == null) {
            throw new IllegalArgumentException(
                    "Unknown update streamer source type: " + sourceType);
        } else if (vehiclePositionSource instanceof JsonConfigurable) {
            ((JsonConfigurable) vehiclePositionSource).configure(graph, config);
        }

        LOG.info("Creating vehicle position updater running every {} seconds : {}", pollingPeriodSeconds, vehiclePositionSource);
    }

    @Override
    public void setup(Graph graph) {
        // Only create a realtime vehicle positions source if none exists already
        if (graph.vehiclePositionPatternMatcher == null) {
            // Add snapshot source to graph
            graph.vehiclePositionPatternMatcher = (new VehiclePositionPatternMatcher(graph));
        }
    }


    /**
     * Repeatedly makes blocking calls to an UpdateStreamer to retrieve new stop time updates, and
     * applies those updates to the graph.
     */
    @Override
    public void runPolling() {
        // Get update lists from update source
        List<VehiclePosition> updates = vehiclePositionSource.getPositions();

        if (updates != null) {
            // Handle updating trip positions via graph writer runnable
            VehiclePositionUpdaterRunnable runnable =
                    new VehiclePositionUpdaterRunnable(updates, feedId);
            updaterManager.execute(runnable);
        }
    }

    @Override
    public void teardown() {
    }

    public String toString() {
        String s = (vehiclePositionSource == null) ? "NONE" : vehiclePositionSource.toString();
        return "Streaming vehicle position updater with update source = " + s;
    }
}
