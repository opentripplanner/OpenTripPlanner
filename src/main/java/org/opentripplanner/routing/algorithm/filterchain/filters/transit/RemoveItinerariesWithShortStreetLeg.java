package org.opentripplanner.routing.algorithm.filterchain.filters.transit;

import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryListFilter;
import org.opentripplanner.street.search.TraverseMode;

/**
 * This filter is useful if you want to remove those results where there is a short bicycle
 * leg followed by parking the bike and taking transit. In such a case you would not need a bike
 * could just walk to the stop instead.
 * <p>
 * TODO: THIS FILTER DOES NOT FOLLOW THE ITINERARY FILTER FRAMEWORK. This filter should implement the
 *       {@link org.opentripplanner.routing.algorithm.filterchain.framework.spi.RemoveItineraryFlagger}.
 *
 * TODO: This filter build on assumptions that is more or less an implementation detail. The
 *       filter should compare itineraries and remove them if a condition is meet, not just
 *       assume that a better option exist. Perhaps the access and egress should be filtered
 *       instead of filtering the transit itineraries?
 */
public class RemoveItinerariesWithShortStreetLeg implements ItineraryListFilter {

  private final double minDistance;
  private final TraverseMode traverseMode;

  public RemoveItinerariesWithShortStreetLeg(double minDistance, TraverseMode traverseMode) {
    this.minDistance = minDistance;
    this.traverseMode = traverseMode;
  }

  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    return itineraries.stream().filter(this::filterItinerariesWithShortStreetLeg).toList();
  }

  private boolean filterItinerariesWithShortStreetLeg(Itinerary itinerary) {
    var hasLegsOfMode = itinerary.getStreetLegs().anyMatch(l -> l.getMode().equals(traverseMode));
    if (hasLegsOfMode && itinerary.hasTransit()) {
      var distance = itinerary
        .getStreetLegs()
        .filter(l -> l.getMode().equals(traverseMode))
        .mapToDouble(Leg::getDistanceMeters)
        .sum();

      return distance > minDistance;
    } else {
      return true;
    }
  }
}
