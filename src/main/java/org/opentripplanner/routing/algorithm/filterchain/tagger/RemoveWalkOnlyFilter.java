package org.opentripplanner.routing.algorithm.filterchain.tagger;

import java.util.function.Predicate;
import org.opentripplanner.model.plan.Itinerary;

/**
 * Filter itineraries and remove all itineraries where all legs are walking.
 */
public class RemoveWalkOnlyFilter implements ItineraryTagger {

    @Override
    public String name() {
        return "remove-walk-only-filter";
    }

    @Override
    public Predicate<Itinerary> predicate() {
        return Itinerary::isWalkingAllTheWay;
    }
}
