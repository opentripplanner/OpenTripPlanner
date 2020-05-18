package org.opentripplanner.routing.algorithm.mapping;

import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.TripPlan;
import org.opentripplanner.routing.api.request.RoutingRequest;

import java.util.List;

public class TripPlanMapper {

    /** This is a utility class with static method only. */
    private TripPlanMapper() { }

    public static TripPlan mapTripPlan(
            RoutingRequest request,
            List<Itinerary> itineraries
    ) {
        Place from;
        Place to;

        if(itineraries.isEmpty()) {
            from = placeFromGeoLocation(request.from);
            to = placeFromGeoLocation(request.to);
        }
        else {
            List<Leg> legs = itineraries.get(0).legs;
            from = legs.get(0).from;
            to = legs.get(legs.size() - 1).to;
        }

        return new TripPlan(from, to, request.getDateTime(), itineraries);
    }

    private static Place placeFromGeoLocation(GenericLocation location) {
        return new Place(location.lat, location.lng, location.label);
    }
}
