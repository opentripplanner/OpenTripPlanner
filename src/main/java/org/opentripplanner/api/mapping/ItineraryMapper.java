package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.ApiItinerary;
import org.opentripplanner.model.plan.Itinerary;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ItineraryMapper {

    public static List<ApiItinerary> mapItineraries(Collection<Itinerary> domain) {
        if(domain == null) { return null; }
        return domain.stream().map(ItineraryMapper::mapItinerary).collect(Collectors.toList());
    }

    public static ApiItinerary mapItinerary(Itinerary domain) {
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
        api.legs = LegMapper.mapLegs(domain.legs);
        api.tooSloped = domain.tooSloped;
        return api;
    }

}
