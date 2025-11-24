package org.opentripplanner.routing.algorithm.mapping;

import java.util.List;
import java.util.OptionalDouble;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.model.plan.walkstep.WalkStep;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;
import org.opentripplanner.street.model.edge.StreetEdge;

public class ItinerariesHelper {

  public static Itinerary decorateItineraryWithRequestData(
    Itinerary itinerary,
    boolean wheelchairEnabled,
    WheelchairPreferences wheelchairPreferences
  ) {
    if (!wheelchairEnabled) {
      return itinerary;
    }

    // Communicate the fact that the only way we were able to get a response
    // was by removing a slope limit.
    OptionalDouble maxSlope = getMaxSlope(itinerary);
    if (maxSlope.isPresent()) {
      return itinerary
        .copyOf()
        .withMaxSlope(wheelchairPreferences.maxSlope(), maxSlope.getAsDouble())
        .build();
    }
    return itinerary;
  }

  private static OptionalDouble getMaxSlope(Itinerary it) {
    return it
      .legs()
      .stream()
      .filter(StreetLeg.class::isInstance)
      .map(StreetLeg.class::cast)
      .map(StreetLeg::listWalkSteps)
      .flatMap(List::stream)
      .map(WalkStep::getEdges)
      .filter(StreetEdge.class::isInstance)
      .map(StreetEdge.class::cast)
      .mapToDouble(StreetEdge::getMaxSlope)
      .max();
  }
}
