package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.util.function.Predicate;
import org.opentripplanner.model.plan.Itinerary;

import java.time.Instant;

public class LatestDepartureTimeFilter implements ItineraryDeletionFlagger {
    private final long limitMs;


    public LatestDepartureTimeFilter(Instant latestDepartureTime) {
        this.limitMs = latestDepartureTime.toEpochMilli();
    }

    @Override
    public String name() {
        return "latest-departure-time-limit";
    }

    @Override
    public Predicate<Itinerary> predicate() {
        return it -> it.startTime().getTimeInMillis() > limitMs;
    }
}
