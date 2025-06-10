package org.opentripplanner.routing.algorithm.filterchain.filters.street;

import java.util.function.Predicate;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.RemoveItineraryFlagger;

/**
 * Filter itineraries and remove all itineraries where all legs are walking.
 */
public class RemoveWalkOnlyFilter implements RemoveItineraryFlagger {

  @Override
  public String name() {
    return "remove-walk-only-filter";
  }

  @Override
  public Predicate<Itinerary> shouldBeFlaggedForRemoval() {
    return itinerary -> itinerary.isWalkOnly();
  }
}
