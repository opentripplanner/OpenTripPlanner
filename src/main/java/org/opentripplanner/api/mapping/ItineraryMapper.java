package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.ApiItinerary;
import org.opentripplanner.model.plan.Itinerary;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ItineraryMapper {
    private final LegMapper legMapper;

    public ItineraryMapper(Locale locale) {
        this.legMapper = new LegMapper(locale);
    }

    public List<ApiItinerary> mapItineraries(Collection<Itinerary> domain) {
        if(domain == null) { return null; }
        return domain.stream().map(this::mapItinerary).collect(Collectors.toList());
    }

    public ApiItinerary mapItinerary(Itinerary domain) {
        if(domain == null) { return null; }
        ApiItinerary api = new ApiItinerary();
        api.duration = domain.duration;
        api.startTime = domain.startTime;
        api.endTime = domain.endTime;
        api.walkTime = domain.walkTime;
        api.transitTime = domain.transitTime;
        api.waitingTime = domain.waitingTime;
        api.walkDistance = domain.walkDistance;
        api.walkLimitExceeded = domain.walkLimitExceeded;
        api.elevationLost = domain.elevationLost;
        api.elevationGained = domain.elevationGained;
        api.transfers = domain.transfers;
        api.fare = domain.fare;
        api.legs = legMapper.mapLegs(domain.legs);
        api.tooSloped = domain.tooSloped;
        return api;
    }

}
