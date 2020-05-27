package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;

import java.util.List;
import java.util.OptionalLong;
import java.util.stream.Collectors;

/**
 * Filter itineraries based on duration, compared with a walk all the way itinerary(if it exist).
 */
public class LongTransitWalkingFilter implements ItineraryFilter {

    @Override
    public String name() {
        return "transit-walking-filter";
    }

    @Override
    public List<Itinerary> filter(List<Itinerary> itineraries) {
        // Find the best walk-all-the-way option
        OptionalLong bestWalkingTimeOp = itineraries
                .stream()
                .filter(Itinerary::isWalkingAllTheWay)
                .mapToLong(itinerary -> itinerary.durationSeconds)
                .min();

        if(bestWalkingTimeOp.isEmpty()) {
            return itineraries;
        }

        final long bestWalkingDuration = bestWalkingTimeOp.getAsLong();

        // Filter away itineraries that spend more time on traveling (except waiting) than
        // walking-all-the-way. We ned to use '<=' not '<' to include the walk-all-the-way
        // itinerary as well.
        return itineraries.stream()
                .filter(it -> it.effectiveDurationSeconds() <= bestWalkingDuration)
                .collect(Collectors.toList());
    }

    @Override
    public boolean removeItineraries() {
        return true;
    }
}
