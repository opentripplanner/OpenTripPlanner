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

    private final VehiclePositionPatternMatcher matcher;

    public VehiclePositionUpdaterRunnable(
            final List<VehiclePosition> updates,
            final VehiclePositionPatternMatcher matcher
    ) {
        Objects.requireNonNull(updates);
        Objects.requireNonNull(matcher);

        this.updates = updates;
        this.matcher = matcher;
    }

    @Override
    public void run(Graph graph) {
        // Apply new vehicle positions
        matcher.applyVehiclePositionUpdates(updates);
    }
}
