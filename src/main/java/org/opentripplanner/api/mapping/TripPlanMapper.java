package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.ApiTripPlan;
import org.opentripplanner.model.plan.TripPlan;

import java.util.Locale;

public class TripPlanMapper {
    private final ItineraryMapper itineraryMapper;
    private final PlaceMapper placeMapper;

    public TripPlanMapper(Locale locale) {
        this.itineraryMapper = new ItineraryMapper(locale);
        this.placeMapper = new PlaceMapper(locale);
    }

    public ApiTripPlan mapTripPlan(TripPlan domain) {
        if(domain == null) { return null; }
        ApiTripPlan api = new ApiTripPlan();
        api.date = domain.date;
        // The origin/destination do not have arrival/depature times; Hence {@code null} is used.
        api.from = placeMapper.mapPlace(domain.from, null, null);
        api.to = placeMapper.mapPlace(domain.to, null, null);
        api.itineraries = itineraryMapper.mapItineraries(domain.itineraries);
        return api;
    }
}
