package org.opentripplanner.routing.algorithm.filterchain.filters.transit;

import java.util.function.Predicate;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.RemoveItineraryFlagger;
import org.opentripplanner.street.search.TraverseMode;

/**
 * This filter is useful if you want to remove those results where there is a short bicycle
 * leg followed by parking the bike and taking transit. In such a case you would not need a bike
 * could just walk to the stop instead.
 * <p>
 * This filter does not follow the regular filter framework as it is intended only for use cases where
 * several queries are combined in the frontend.
 * <p>
 * Example: you have two queries for bike+transit and walk+transit each. Both give you very short legs
 * to reach a train station. A user would not expect to see a bike+transit shorter than 200m leg when it's
 * presented right next to a walk+transit leg of the same length.
 * <p>
 * In other words, this offloads the comparison part of the filter chain to a system outside of OTP and
 * that is the reason for this non-standard approach.
 */
public class RemoveItinerariesWithShortStreetLeg implements RemoveItineraryFlagger {

  private final double minDistance;
  private final TraverseMode traverseMode;

  public RemoveItinerariesWithShortStreetLeg(double minDistance, TraverseMode traverseMode) {
    this.minDistance = minDistance;
    this.traverseMode = traverseMode;
  }

  @Override
  public String name() {
    return "remove-itineraries-with-short-street-leg";
  }

  @Override
  public Predicate<Itinerary> shouldBeFlaggedForRemoval() {
    return this::removeItineraryWithShortStreetLeg;
  }

  private boolean removeItineraryWithShortStreetLeg(Itinerary itinerary) {
    var hasLegsOfMode = itinerary.streetLegs().anyMatch(l -> l.getMode().equals(traverseMode));
    if (hasLegsOfMode && itinerary.hasTransit()) {
      var distance = itinerary
        .streetLegs()
        .filter(l -> l.getMode().equals(traverseMode))
        .mapToDouble(Leg::distanceMeters)
        .sum();

      return distance <= minDistance;
    } else {
      return false;
    }
  }
}
