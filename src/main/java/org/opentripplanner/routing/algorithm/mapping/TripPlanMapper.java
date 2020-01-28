package org.opentripplanner.routing.algorithm.mapping;

import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.Place;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.core.RoutingRequest;

import java.util.List;

public class TripPlanMapper {

    /** This is a utility class with static method only. */
    private TripPlanMapper() { }

    public static TripPlan mapTripPlan(RoutingRequest request, List<Itinerary> itineraries) {
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

        TripPlan tripPlan = new TripPlan(from, to, request.getDateTime());

        // TODO OTP2 - This does not make sense
        //tripPlan.itinerary = limitNumberOfItineraries(itineraries, request.numItineraries);

        tripPlan.itinerary = itineraries;

        return tripPlan;
    }

    private static Place placeFromGeoLocation(GenericLocation location) {
        return new Place(location.lng, location.lat, location.label);
    }
}
