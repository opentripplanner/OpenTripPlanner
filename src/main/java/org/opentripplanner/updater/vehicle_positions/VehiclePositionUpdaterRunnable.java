package org.opentripplanner.updater.vehicle_positions;

import com.google.common.base.Preconditions;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class VehiclePositionUpdaterRunnable implements GraphWriterRunnable {
    private static final Logger LOG =
            LoggerFactory.getLogger(VehiclePositionUpdaterRunnable.class);

    /**
     * The list with vehicle positions (not associated with patterns)
     */
    private final List<VehiclePosition> updates;

    private final String feedId;

    public VehiclePositionUpdaterRunnable(final List<VehiclePosition> updates, final String feedId) {
        // Preconditions
        Preconditions.checkNotNull(updates);
        Preconditions.checkNotNull(feedId);

        // Set fields
        this.updates = updates;
        this.feedId = feedId;
    }
    /**
     * This function is executed to modify patterns
     *
     * @param graph
     */
    @Override
    public void run(Graph graph) {
        // Apply updates to graph using realtime snapshot source
        VehiclePositionPatternMatcher vehiclePositionPatternMatcher = graph.vehiclePositionPatternMatcher;
        if (vehiclePositionPatternMatcher != null) {
            vehiclePositionPatternMatcher.applyVehiclePositionUpdates(updates, feedId);
        } else {
            LOG.error("Could not find realtime data snapshot source in graph."
                    + " The following updates are not applied: {}", updates);
        }
    }
}
