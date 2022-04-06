package org.opentripplanner.updater.vehicle_positions;

import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphWriterRunnable;

public record VehiclePositionUpdaterRunnable(
  List<VehiclePosition> updates,
  VehiclePositionPatternMatcher matcher
)
  implements GraphWriterRunnable {
  public VehiclePositionUpdaterRunnable {
    Objects.requireNonNull(updates);
    Objects.requireNonNull(matcher);
  }

  @Override
  public void run(Graph graph) {
    // Apply new vehicle positions
    matcher.applyVehiclePositionUpdates(updates);
  }
}
