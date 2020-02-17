package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.ApiTripPlan;
import org.opentripplanner.model.plan.TripPlan;

import java.util.Locale;

public class TripPlanMapper {
    private final ItineraryMapper itineraryMapper;

    public TripPlanMapper(Locale locale) {
        this.itineraryMapper = new ItineraryMapper(locale);
    }

    public ApiTripPlan mapTripPlan(TripPlan domain) {
        if(domain == null) { return null; }
        ApiTripPlan api = new ApiTripPlan();
        api.date = domain.date;
        api.from = PlaceMapper.mapPlace(domain.from);
        api.to = PlaceMapper.mapPlace(domain.to);
        api.itinerary = itineraryMapper.mapItineraries(domain.itineraries);
        return api;
    }
}
