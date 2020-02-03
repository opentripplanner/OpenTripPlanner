package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.ApiTripPlan;
import org.opentripplanner.model.plan.TripPlan;

public class TripPlanMapper {

    public static ApiTripPlan mapTripPlan(TripPlan domain) {
        if(domain == null) { return null; }
        ApiTripPlan api = new ApiTripPlan();
        api.date = domain.date;
        api.from = PlaceMapper.mapPlace(domain.from);
        api.to = PlaceMapper.mapPlace(domain.to);
        api.itinerary = ItineraryMapper.mapItineraries(domain.itineraries);
        return api;
    }
}
