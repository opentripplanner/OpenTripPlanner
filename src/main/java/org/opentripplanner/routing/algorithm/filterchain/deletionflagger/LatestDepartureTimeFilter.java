package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.util.function.Predicate;
import org.opentripplanner.model.plan.Itinerary;

import java.time.Instant;

public class LatestDepartureTimeFilter implements ItineraryDeletionFlagger {
    public static final String NAME = "latest-departure-time-limit";

    private final long limitMs;

    public LatestDepartureTimeFilter(Instant latestDepartureTime) {
        this.limitMs = latestDepartureTime.toEpochMilli();
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean skipAlreadyFlaggedItineraries() {
        return false;
    }

    @Override
    public Predicate<Itinerary> predicate() {
        return it -> it.startTime().getTimeInMillis() > limitMs;
    }
}
