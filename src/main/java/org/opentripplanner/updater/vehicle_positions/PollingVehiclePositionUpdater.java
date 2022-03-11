package org.opentripplanner.updater.vehicle_positions;

import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import java.io.File;
import java.util.List;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.opentripplanner.updater.WriteToGraphCallback;
import org.opentripplanner.updater.vehicle_positions.params.VehiclePositionsUpdaterFileParameters;
import org.opentripplanner.updater.vehicle_positions.params.VehiclePositionsUpdaterHttpParameters;
import org.opentripplanner.updater.vehicle_positions.params.VehiclePositionsUpdaterParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Add vehicle positions to OTP patterns via a realtime source *
 * <pre>
 * rt.type = vehicle-positions
 * rt.frequencySec = 60
 * rt.sourceType = gtfs-http
 * rt.url = http://host.tld/path
 * rt.feedId = TA
 * </pre>
 */
public class PollingVehiclePositionUpdater extends PollingGraphUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(PollingVehiclePositionUpdater.class);

    /**
     * Parent update manager. Is used to execute graph writer runnables.
     */
    private WriteToGraphCallback saveResultOnGraph;

    /**
     * Update streamer
     */
    private final VehiclePositionSource vehiclePositionSource;
    private VehiclePositionPatternMatcher vehiclePositionPatternMatcher;

    @Override
    public void setGraphUpdaterManager(WriteToGraphCallback saveResultOnGraph) {
        this.saveResultOnGraph = saveResultOnGraph;
    }

    public PollingVehiclePositionUpdater(VehiclePositionsUpdaterParameters params) {
        super(params);
        // Create update streamer from params
        if (params instanceof VehiclePositionsUpdaterHttpParameters) {
            var p = (VehiclePositionsUpdaterHttpParameters) params;
            vehiclePositionSource = new GtfsRealtimeHttpVehiclePositionSource(p.feedId, p.url);
        }
        else {
            var p = (VehiclePositionsUpdaterFileParameters) params;
            vehiclePositionSource =
                    new GtfsRealtimeFileVehiclePositionSource(p.feedId, new File(p.path));
        }

        LOG.info(
                "Creating vehicle position updater running every {} seconds : {}",
                pollingPeriodSeconds, vehiclePositionSource
        );
    }

    @Override
    public void setup(Graph graph) {
        var index = graph.index;
        vehiclePositionPatternMatcher =
                new VehiclePositionPatternMatcher(
                        index::getTripForId,
                        index::getPatternForTrip,
                        graph.getVehiclePositionService()
                );
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
            var runnable =
                    new VehiclePositionUpdaterRunnable(updates, vehiclePositionSource.getFeedId(),
                            vehiclePositionPatternMatcher
                    );
            saveResultOnGraph.execute(runnable);
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
