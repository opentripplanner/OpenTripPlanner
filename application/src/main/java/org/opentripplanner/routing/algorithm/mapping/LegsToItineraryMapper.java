package org.opentripplanner.routing.algorithm.mapping;

import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.street.model.path.ElevationChange;

public class LegsToItineraryMapper {

  public static Optional<Itinerary> map(
    List<Leg> legs,
    boolean arrivedAtDestinationWithRentedVehicle,
    @Nullable ElevationChange elevationChange
  ) {
    if (legs.isEmpty()) {
      return Optional.empty();
    }
    var cost = Cost.costOfSeconds(legs.stream().mapToDouble(Leg::generalizedCost).sum());
    var builder = Itinerary.ofDirect(legs).withGeneralizedCost(cost);

    builder.withArrivedAtDestinationWithRentedVehicle(arrivedAtDestinationWithRentedVehicle);
    if (elevationChange != null) {
      builder.addElevationChange(elevationChange);
    }

    return Optional.of(builder.build());
  }
}
