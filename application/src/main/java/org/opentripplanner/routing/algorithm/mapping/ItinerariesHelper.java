package org.opentripplanner.routing.algorithm.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.model.plan.walkstep.WalkStep;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;
import org.opentripplanner.street.model.edge.StreetEdge;

public class ItinerariesHelper {

  public static List<Itinerary> decorateItinerariesWithRequestData(
    List<Itinerary> itineraries,
    boolean wheelchairEnabled,
    WheelchairPreferences wheelchairPreferences
  ) {
    if (!wheelchairEnabled) {
      return itineraries;
    }
    var result = new ArrayList<Itinerary>();
    boolean dirty = false;

    for (Itinerary it : itineraries) {
      // Communicate the fact that the only way we were able to get a response
      // was by removing a slope limit.
      OptionalDouble maxSlope = getMaxSlope(it);
      if (maxSlope.isPresent()) {
        dirty = true;
        itineraries.add(
          it.copyOf().withMaxSlope(wheelchairPreferences.maxSlope(), maxSlope.getAsDouble()).build()
        );
      } else {
        result.add(it);
      }
    }
    return dirty ? result : itineraries;
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
