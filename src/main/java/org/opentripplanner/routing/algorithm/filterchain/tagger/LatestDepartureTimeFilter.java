package org.opentripplanner.routing.algorithm.filterchain.tagger;

import org.opentripplanner.model.plan.Itinerary;

import java.time.Instant;
import java.util.List;

public class LatestDepartureTimeFilter implements ItineraryTagger {
    private final long limitMs;


    public LatestDepartureTimeFilter(Instant latestDepartureTime) {
        this.limitMs = latestDepartureTime.toEpochMilli();
    }

    @Override
    public String name() {
        return "latest-departure-time-limit";
    }

    @Override
    public void tagItineraries(List<Itinerary> itineraries) {
        itineraries
                .stream()
                .filter(it -> it.startTime().getTimeInMillis() > limitMs)
                .forEach(it -> it.markAsDeleted(notice()));
    }
}
