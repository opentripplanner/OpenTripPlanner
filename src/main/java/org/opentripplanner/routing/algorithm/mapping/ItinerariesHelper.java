package org.opentripplanner.routing.algorithm.mapping;

import java.util.List;
import java.util.OptionalDouble;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.model.plan.WalkStep;
import org.opentripplanner.routing.api.request.preference.WheelchairAccessibilityPreferences;
import org.opentripplanner.routing.edgetype.StreetEdge;

public class ItinerariesHelper {

  public static void decorateItinerariesWithRequestData(
    List<Itinerary> itineraries,
    boolean wheelchairEnabled,
    WheelchairAccessibilityPreferences wheelchairAccessibility
  ) {
    for (Itinerary it : itineraries) {
      if (wheelchairEnabled) {
        // Communicate the fact that the only way we were able to get a response
        // was by removing a slope limit.
        OptionalDouble maxSlope = getMaxSlope(it);
        if (maxSlope.isPresent()) {
          it.setTooSloped(maxSlope.getAsDouble() > wheelchairAccessibility.maxSlope());
          it.setMaxSlope(maxSlope.getAsDouble());
        }
      }
    }
  }

  private static OptionalDouble getMaxSlope(Itinerary it) {
    return it
      .getLegs()
      .stream()
      .filter(StreetLeg.class::isInstance)
      .map(StreetLeg.class::cast)
      .map(StreetLeg::getWalkSteps)
      .flatMap(List::stream)
      .map(WalkStep::getEdges)
      .filter(StreetEdge.class::isInstance)
      .map(StreetEdge.class::cast)
      .mapToDouble(StreetEdge::getMaxSlope)
      .max();
  }
}
