package org.opentripplanner.routing.algorithm.mapping;

import static org.opentripplanner.ext.realtimeresolver.RealtimeResolver.populateLegsWithRealtime;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.model.plan.pagecursor.PageCursor;
import org.opentripplanner.model.plan.pagecursor.PageCursorFactory;
import org.opentripplanner.model.plan.pagecursor.PageType;
import org.opentripplanner.raptor.api.request.SearchParams;
import org.opentripplanner.routing.algorithm.filterchain.deletionflagger.NumItinerariesFilterResults;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.api.response.TripSearchMetadata;
import org.opentripplanner.routing.framework.DebugTimingAggregator;
import org.opentripplanner.transit.service.TransitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutingResponseMapper {

  private static final Logger LOG = LoggerFactory.getLogger(RoutingResponseMapper.class);

  public static RoutingResponse map(
    RouteRequest request,
    ZonedDateTime transitSearchTimeZero,
    SearchParams raptorSearchParamsUsed,
    Duration searchWindowForNextSearch,
    NumItinerariesFilterResults numItinerariesFilterResults,
    List<Itinerary> itineraries,
    Set<RoutingError> routingErrors,
    DebugTimingAggregator debugTimingAggregator,
    TransitService transitService
  ) {
    // Search is performed without realtime, but we still want to
    // include realtime information in the result
    if (
      request.preferences().transit().ignoreRealtimeUpdates() && OTPFeature.RealtimeResolver.isOn()
    ) {
      populateLegsWithRealtime(itineraries, transitService);
    }

    // Create response
    var tripPlan = TripPlanMapper.mapTripPlan(request, itineraries);

    var factory = mapIntoPageCursorFactory(
      request.itinerariesSortOrder(),
      transitSearchTimeZero,
      raptorSearchParamsUsed,
      searchWindowForNextSearch,
      numItinerariesFilterResults,
      request.pageCursor() == null ? null : request.pageCursor().type
    );

    PageCursor nextPageCursor = factory.nextPageCursor();
    PageCursor prevPageCursor = factory.previousPageCursor();

    if (LOG.isDebugEnabled()) {
      logPagingInformation(request.pageCursor(), prevPageCursor, nextPageCursor, routingErrors);
    }

    var metadata = createTripSearchMetadata(
      request,
      raptorSearchParamsUsed,
      numItinerariesFilterResults == null
        ? null
        : numItinerariesFilterResults.firstRemovedDepartureTime,
      numItinerariesFilterResults == null
        ? null
        : numItinerariesFilterResults.firstRemovedArrivalTime
    );

    return new RoutingResponse(
      tripPlan,
      prevPageCursor,
      nextPageCursor,
      metadata,
      List.copyOf(routingErrors),
      debugTimingAggregator
    );
  }

  public static PageCursorFactory mapIntoPageCursorFactory(
    SortOrder sortOrder,
    ZonedDateTime transitSearchTimeZero,
    SearchParams raptorSearchParamsUsed,
    Duration searchWindowNextSearch,
    NumItinerariesFilterResults numItinerariesFilterResults,
    @Nullable PageType currentPageType
  ) {
    Objects.requireNonNull(sortOrder);
    Objects.requireNonNull(transitSearchTimeZero);

    var factory = new PageCursorFactory(sortOrder, searchWindowNextSearch);

    // No transit search performed
    if (raptorSearchParamsUsed == null) {
      return factory;
    }

    assertRequestPrerequisites(raptorSearchParamsUsed);

    factory =
      mapSearchParametersIntoFactory(
        factory,
        transitSearchTimeZero,
        raptorSearchParamsUsed,
        currentPageType
      );

    if (numItinerariesFilterResults != null) {
      factory = factory.withRemovedItineraries(numItinerariesFilterResults);
    }
    return factory;
  }

  private static PageCursorFactory mapSearchParametersIntoFactory(
    PageCursorFactory factory,
    ZonedDateTime transitSearchTimeZero,
    SearchParams raptorSearchParamsUsed,
    PageType currentPageType
  ) {
    Instant edt = transitSearchTimeZero
      .plusSeconds(raptorSearchParamsUsed.earliestDepartureTime())
      .toInstant();

    Instant lat = raptorSearchParamsUsed.isLatestArrivalTimeSet()
      ? transitSearchTimeZero.plusSeconds(raptorSearchParamsUsed.latestArrivalTime()).toInstant()
      : null;

    var searchWindowUsed = Duration.ofSeconds(raptorSearchParamsUsed.routerSearchWindowInSeconds());

    return factory.withOriginalSearch(currentPageType, edt, lat, searchWindowUsed);
  }

  @Nullable
  private static TripSearchMetadata createTripSearchMetadata(
    RouteRequest request,
    SearchParams searchParams,
    Instant firstRemovedDepartureTime,
    Instant firstRemovedArrivalTime
  ) {
    if (searchParams == null) {
      return null;
    }

    Instant reqTime = request.dateTime();

    if (request.arriveBy()) {
      return TripSearchMetadata.createForArriveBy(
        reqTime,
        searchParams.searchWindowInSeconds(),
        firstRemovedArrivalTime
      );
    } else {
      return TripSearchMetadata.createForDepartAfter(
        reqTime,
        searchParams.searchWindowInSeconds(),
        firstRemovedDepartureTime
      );
    }
  }

  private static void assertRequestPrerequisites(SearchParams raptorSearchParamsUsed) {
    if (!raptorSearchParamsUsed.isSearchWindowSet()) {
      throw new IllegalStateException("SearchWindow not set");
    }
    if (!raptorSearchParamsUsed.isEarliestDepartureTimeSet()) {
      throw new IllegalStateException("Earliest departure time not set");
    }
  }

  private static void logPagingInformation(
    PageCursor currentPageCursor,
    PageCursor prevPageCursor,
    PageCursor nextPageCursor,
    Set<RoutingError> errors
  ) {
    LOG.debug("PageCursor current  : {}", currentPageCursor);
    LOG.debug("PageCursor previous : {}", prevPageCursor);
    LOG.debug("PageCursor next ... : {}", nextPageCursor);
    LOG.debug("Errors ............ : {}", errors);
  }
}
