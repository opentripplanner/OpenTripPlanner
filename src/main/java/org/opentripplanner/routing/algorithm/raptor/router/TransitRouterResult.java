package org.opentripplanner.routing.algorithm.raptor.router;

import java.time.Instant;
import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.transit.raptor.api.request.SearchParams;

public class TransitRouterResult {

    private final List<Itinerary> itineraries;
    private final SearchParams searchParams;

    public TransitRouterResult(
            List<Itinerary> itineraries,
            SearchParams searchParams
    ) {
        this.itineraries = itineraries;
        this.searchParams = searchParams;
    }

    public List<Itinerary> getItineraries() {
        return itineraries;
    }

    public SearchParams getSearchParams() {
        return searchParams;
    }
}
