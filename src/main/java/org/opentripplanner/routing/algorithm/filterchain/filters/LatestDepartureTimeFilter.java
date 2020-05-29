package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class LatestDepartureTimeFilter implements ItineraryFilter {
    private final long limitMs;


    public LatestDepartureTimeFilter(Instant latestDepartureTime) {
        this.limitMs = latestDepartureTime.toEpochMilli();
    }

    @Override
    public String name() {
        return "latest-departure-time-limit";
    }

    @Override
    public List<Itinerary> filter(List<Itinerary> itineraries) {
        return itineraries
                .stream()
                .filter(it -> it.startTime().getTimeInMillis() <= limitMs)
                .collect(Collectors.toList());
    }

    @Override
    public boolean removeItineraries() {
        return true;
    }

}
