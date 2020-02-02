package org.opentripplanner.routing.algorithm.mapping;

import org.opentripplanner.api.model.ApiItinerary;
import org.opentripplanner.api.model.ApiLeg;
import org.opentripplanner.api.model.ApiPlace;
import org.opentripplanner.api.model.ApiTripPlan;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.core.RoutingRequest;

import java.util.List;

public class TripPlanMapper {

    /** This is a utility class with static method only. */
    private TripPlanMapper() { }

    public static ApiTripPlan mapTripPlan(
            RoutingRequest request,
            List<ApiItinerary> itineraries
    ) {
        ApiPlace from;
        ApiPlace to;

        if(itineraries.isEmpty()) {
            from = placeFromGeoLocation(request.from);
            to = placeFromGeoLocation(request.to);
        }
        else {
            List<ApiLeg> legs = itineraries.get(0).legs;
            from = legs.get(0).from;
            to = legs.get(legs.size() - 1).to;
        }

        return new ApiTripPlan(from, to, request.getDateTime(), itineraries);
    }

    private static ApiPlace placeFromGeoLocation(GenericLocation location) {
        return new ApiPlace(location.lng, location.lat, location.label);
    }
}
