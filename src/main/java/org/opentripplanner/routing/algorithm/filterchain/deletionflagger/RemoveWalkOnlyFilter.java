package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.util.function.Predicate;
import org.opentripplanner.model.plan.Itinerary;

/**
 * Filter itineraries and remove all itineraries where all legs are walking.
 */
public class RemoveWalkOnlyFilter implements ItineraryDeletionFlagger {

  @Override
  public String name() {
    return "remove-walk-only-filter";
  }

  @Override
  public Predicate<Itinerary> shouldBeFlaggedForRemoval() {
    return Itinerary::isWalkingAllTheWay;
  }
}
