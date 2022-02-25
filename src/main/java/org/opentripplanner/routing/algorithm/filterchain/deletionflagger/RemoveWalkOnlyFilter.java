package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import org.opentripplanner.model.plan.Itinerary;

import java.util.function.Predicate;

/**
 * Filter itineraries and remove all itineraries where all legs are walking.
 */
public class RemoveWalkOnlyFilter implements ItineraryDeletionFlagger {

    @Override
    public String name() {
        return "remove-walk-only-filter";
    }

    @Override
    public Predicate<Itinerary> predicate() {
        return Itinerary::isWalkingAllTheWay;
    }
}
