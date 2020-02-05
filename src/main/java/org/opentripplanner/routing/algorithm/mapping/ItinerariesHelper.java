package org.opentripplanner.routing.algorithm.mapping;

import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.routing.core.RoutingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ItinerariesHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ItinerariesHelper.class);

    // TODO OTP2 - This was used to limit the number of itineraries, but we need a better
    //           - way to do this. The code is keept here for reference .
    //           - DELETE AFTER 2020-05-05
    public static List<Itinerary> limitNumberOfItineraries(List<Itinerary> itineraries, int max) {
        return itineraries.stream()
                .sorted(Comparator.comparing(i -> i.endTime))
                .limit(max)
                .collect(Collectors.toList());
    }

    public static List<Itinerary> filterAwayLongWalkingTransit(List<Itinerary> itineraries) {
        List<Itinerary> result = new ArrayList<>();

        // Find the best non-transit (e.g. walk/bike-only) option time
        long bestNonTransitWalkTime = itineraries.stream()
                .filter(it -> it.transitTime == 0)
                .mapToLong(it -> it.walkTime)
                .min()
                .orElse(Long.MAX_VALUE);

        // Filter itineraries
        // If this is a transit option whose walk/bike time is greater than
        // that of the walk/bike-only option, do not include in plan
        for (Itinerary it : itineraries) {
            if(it.transitTime <= 0 || it.walkTime < bestNonTransitWalkTime) {
                result.add(it);
            }
        }
        return result;
    }

    public static void decorateItinerariesWithRequestData(
            List<Itinerary> itineraries,
            RoutingRequest request
    ) {
        for (Itinerary it : itineraries) {
            // Communicate the fact that the only way we were able to get a response
            // was by removing a slope limit.
            it.tooSloped = request.rctx.slopeRestrictionRemoved;

            // fix up from/to on first/last legs
            if (it.legs.size() == 0) {
                LOG.warn("itinerary has no legs");
                continue;
            }
            Leg firstLeg = it.legs.get(0);
            firstLeg.from.orig = request.from.label;
            Leg lastLeg = it.legs.get(it.legs.size() - 1);
            lastLeg.to.orig = request.to.label;
        }
    }
}
