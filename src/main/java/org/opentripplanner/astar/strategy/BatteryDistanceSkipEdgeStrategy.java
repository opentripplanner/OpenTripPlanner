package org.opentripplanner.astar.strategy;

import java.util.Optional;
import org.opentripplanner.astar.spi.AStarEdge;
import org.opentripplanner.astar.spi.AStarState;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;
import org.opentripplanner.service.vehiclerental.street.StreetVehicleRentalLink;

/**
 * Skips Edges when the available battery distance of a vehicle is less than the accumulated driving
 * distance of the same vehicle
 */
public class BatteryDistanceSkipEdgeStrategy<
  State extends AStarState<State, Edge, ?>, Edge extends AStarEdge<State, Edge, ?>
>
  implements SkipEdgeStrategy<State, Edge> {

  @Override
  public boolean shouldSkipEdge(State current, Edge edge) {
    if (edge instanceof StreetVehicleRentalLink) {
      Optional<Double> currentRangeMeters =
        ((StreetVehicleRentalLink) edge).getCurrentRangeMeters();

      if (currentRangeMeters.isEmpty()) {
        return false;
      }
      double batteryDistance =
        ((org.opentripplanner.street.search.state.State) current).batteryDistance;
      return currentRangeMeters.get() < batteryDistance;
    }
    return false;
  }
}
