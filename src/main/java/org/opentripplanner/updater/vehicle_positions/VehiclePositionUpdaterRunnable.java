package org.opentripplanner.updater.vehicle_positions;

import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphWriterRunnable;

public class VehiclePositionUpdaterRunnable implements GraphWriterRunnable {

    /**
     * The list with vehicle positions (not associated with patterns)
     */
    private final List<VehiclePosition> updates;

    private final String feedId;

    private final VehiclePositionPatternMatcher matcher;

    public VehiclePositionUpdaterRunnable(
            final List<VehiclePosition> updates,
            final String feedId,
            final VehiclePositionPatternMatcher matcher
    ) {
        // Preconditions
        Objects.requireNonNull(updates);
        Objects.requireNonNull(feedId);
        Objects.requireNonNull(matcher);

        // Set fields
        this.updates = updates;
        this.feedId = feedId;
        this.matcher = matcher;
    }

    /**
     * This function is executed to modify patterns
     *
     * @param graph
     */
    @Override
    public void run(Graph graph) {
        // Reset seen trip IDs before populating the set again
        matcher.wipeSeenTripIds();

        // Apply new vehicle positions
        // This will update the internal seenTripIds
        matcher.applyVehiclePositionUpdates(updates, feedId);

        // "clean" all patterns, removing all vehicles not "seen" in the
        // previous step (stored in the seen trip IDs)
        matcher.cleanPatternVehiclePositions(feedId);
    }
}
