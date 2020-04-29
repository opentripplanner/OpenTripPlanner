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

        api.duration = (long) domain.durationSeconds;
        api.startTime = domain.startTime();
        api.endTime = domain.endTime();
        api.walkTime = domain.nonTransitTimeSeconds;
        api.transitTime = domain.transitTimeSeconds;
        api.waitingTime = domain.waitingTimeSeconds;
        api.walkDistance = domain.nonTransitDistanceMeters;
        api.walkLimitExceeded = domain.nonTransitLimitExceeded;
        api.elevationLost = domain.elevationLost;
        api.elevationGained = domain.elevationGained;
        api.transfers = domain.nTransfers;
        api.tooSloped = domain.tooSloped;
        api.fare = domain.fare;
        api.legs = legMapper.mapLegs(domain.legs);
        api.systemNotices = SystemNoticeMapper.mapSystemNotices(domain.systemNotices);

        return api;
    }

}
