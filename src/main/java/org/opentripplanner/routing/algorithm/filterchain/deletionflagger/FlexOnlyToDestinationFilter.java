package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;

/**
 * Remove all itineraries where all the final leg is more than x minutes of walking.
 */
public class FlexOnlyToDestinationFilter implements ItineraryListFilter {

  private final long maxWalkSeconds = Duration.ofMinutes(2).toSeconds();

  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    return itineraries
      .stream()
      .filter(it -> {
        var lastLeg = it.lastLeg();
        boolean lastLegIsLongWalk =
          lastLeg.isWalkingLeg() && lastLeg.getDuration().toSeconds() > maxWalkSeconds;

        var lastLegIsFlex = it
          .getLegs()
          .stream()
          .filter(l -> l.isTransitLeg() || l.isFlexibleTrip())
          // get last element of stream
          .reduce((first, second) -> second)
          .map(Leg::isFlexibleTrip)
          .orElse(false);

        if (lastLegIsFlex) {
          return !lastLegIsLongWalk;
        } else {
          return true;
        }
      })
      .toList();
  }
}
