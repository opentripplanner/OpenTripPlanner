package org.opentripplanner.routing.algorithm.itineraryfilters;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.ItineraryFilter;
import org.opentripplanner.routing.core.RoutingRequest;

import java.util.ArrayList;
import java.util.List;

public class FilterAwayLongWalkingTransit implements ItineraryFilter {

    @Override
    public List<Itinerary> filter(
            RoutingRequest request, List<Itinerary> itineraries
    ) {
        // This filer is only applied to transit requests
        if(!request.modes.isTransit()) {
            return itineraries;
        }

        List<Itinerary> result = new ArrayList<>();

        // Find the best non-transit (e.g. walk/bike-only) option time
        long bestWalkingTime = itineraries
                .stream()
                .filter(Itinerary::isWalkingAllTheWay)
                .mapToLong(it -> it.walkTime)
                .min()
                .orElse(Long.MAX_VALUE);

        // Filter itineraries
        // If this is a transit option whose walk/bike time is greater than
        // that of the walk/bike-only option, do not include in plan
        for (Itinerary it : itineraries) {
            if (it.transitTime <= 0 || it.walkTime < bestWalkingTime) {
                result.add(it);
            }
        }
        return result;
    }
}
