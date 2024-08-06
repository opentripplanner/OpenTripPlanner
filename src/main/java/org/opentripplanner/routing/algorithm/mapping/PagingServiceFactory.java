package org.opentripplanner.routing.algorithm.mapping;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.paging.cursor.PageCursorInput;
import org.opentripplanner.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.raptor.api.request.SearchParams;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParameters;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.service.paging.PagingService;

public class PagingServiceFactory {

  public static PagingService createPagingService(
    Instant searchStartTime,
    TransitTuningParameters transitTuningParameters,
    RaptorTuningParameters raptorTuningParameters,
    RouteRequest request,
    SearchParams raptorSearchParamsUsed,
    PageCursorInput pageCursorInput,
    List<Itinerary> itineraries
  ) {
    return new PagingService(
      transitTuningParameters.pagingSearchWindowAdjustments(),
      raptorTuningParameters.dynamicSearchWindowCoefficients().minWindow(),
      raptorTuningParameters.dynamicSearchWindowCoefficients().maxWindow(),
      searchWindowOf(raptorSearchParamsUsed),
      edt(searchStartTime, raptorSearchParamsUsed),
      lat(searchStartTime, raptorSearchParamsUsed),
      request.itinerariesSortOrder(),
      request.arriveBy(),
      request.numItineraries(),
      request.pageCursor(),
      pageCursorInput,
      itineraries
    );
  }

  static Duration searchWindowOf(SearchParams searchParamsUsed) {
    if (searchParamsUsed == null || !searchParamsUsed.isSearchWindowSet()) {
      return null;
    }
    return Duration.ofSeconds(searchParamsUsed.searchWindowInSeconds());
  }

  static Instant edt(Instant transitSearchStartTime, SearchParams searchParamsUsed) {
    if (searchParamsUsed == null || !searchParamsUsed.isEarliestDepartureTimeSet()) {
      return null;
    }
    return transitSearchStartTime.plusSeconds(searchParamsUsed.earliestDepartureTime());
  }

  static Instant lat(Instant transitSearchStartTime, SearchParams searchParamsUsed) {
    if (searchParamsUsed == null || !searchParamsUsed.isLatestArrivalTimeSet()) {
      return null;
    }
    return transitSearchStartTime.plusSeconds(searchParamsUsed.latestArrivalTime());
  }
}
