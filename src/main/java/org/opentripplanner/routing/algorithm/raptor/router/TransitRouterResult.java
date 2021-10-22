package org.opentripplanner.routing.algorithm.raptor.router;

import java.time.Instant;
import java.util.List;
import org.opentripplanner.model.plan.Itinerary;

public class TransitRouterResult {

    private final List<Itinerary> itineraries;
    private final Instant filterOnLatestDepartureTime;
    private final int searchWindowUsedInSeconds;

    public TransitRouterResult(
            List<Itinerary> itineraries,
            Instant filterOnLatestDepartureTime,
            int searchWindowUsedInSeconds
    ) {
        this.itineraries = itineraries;
        this.filterOnLatestDepartureTime = filterOnLatestDepartureTime;
        this.searchWindowUsedInSeconds = searchWindowUsedInSeconds;
    }

    public List<Itinerary> getItineraries() {
        return itineraries;
    }

    public Instant getFilterOnLatestDepartureTime() {
        return filterOnLatestDepartureTime;
    }

    public int getSearchWindowUsedInSeconds() {
        return searchWindowUsedInSeconds;
    }
}
