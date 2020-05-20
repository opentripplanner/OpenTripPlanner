package org.opentripplanner.routing.algorithm.mapping;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ItinerariesHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ItinerariesHelper.class);

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
