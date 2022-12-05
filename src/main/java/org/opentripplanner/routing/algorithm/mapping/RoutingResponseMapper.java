package org.opentripplanner.routing.algorithm.mapping;

import static org.opentripplanner.ext.realtimeresolver.RealtimeResolver.populateLegsWithRealtime;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.model.plan.pagecursor.PageCursor;
import org.opentripplanner.model.plan.pagecursor.PageCursorFactory;
import org.opentripplanner.model.plan.pagecursor.PageType;
import org.opentripplanner.raptor.api.request.SearchParams;
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
    SearchParams searchParams,
    Duration searchWindowForNextSearch,
    Itinerary firstRemovedItinerary,
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
      searchParams,
      searchWindowForNextSearch,
      firstRemovedItinerary,
      request.pageCursor() == null ? null : request.pageCursor().type
    );

    PageCursor nextPageCursor = factory.nextPageCursor();
    PageCursor prevPageCursor = factory.previousPageCursor();

    if (LOG.isDebugEnabled()) {
      logPagingInformation(request.pageCursor(), prevPageCursor, nextPageCursor, routingErrors);
    }

    var metadata = createTripSearchMetadata(request, searchParams, firstRemovedItinerary);

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
    SearchParams searchParams,
    Duration searchWindowNextSearch,
    Itinerary firstRemovedItinerary,
    @Nullable PageType currentPageType
  ) {
    var factory = new PageCursorFactory(sortOrder, searchWindowNextSearch);

    if (searchParams != null) {
      if (!searchParams.isSearchWindowSet()) {
        LOG.debug("SearchWindow not set");
        return factory;
      }
      if (!searchParams.isEarliestDepartureTimeSet()) {
        LOG.debug("Earliest departure time not set");
        return factory;
      }

      long t0 = transitSearchTimeZero.toEpochSecond();
      var edt = Instant.ofEpochSecond(t0 + searchParams.earliestDepartureTime());
      var lat = searchParams.isLatestArrivalTimeSet()
        ? Instant.ofEpochSecond(t0 + searchParams.latestArrivalTime())
        : null;
      var searchWindow = Duration.ofSeconds(searchParams.searchWindowInSeconds());
      factory.withOriginalSearch(currentPageType, edt, lat, searchWindow);
    }

    if (firstRemovedItinerary != null) {
      factory.withRemovedItineraries(
        firstRemovedItinerary.startTime().toInstant(),
        firstRemovedItinerary.endTime().toInstant()
      );
    }

    return factory;
  }

  @Nullable
  private static TripSearchMetadata createTripSearchMetadata(
    RouteRequest request,
    SearchParams searchParams,
    Itinerary firstRemovedItinerary
  ) {
    if (searchParams == null) {
      return null;
    }

    Instant reqTime = request.dateTime();

    if (request.arriveBy()) {
      return TripSearchMetadata.createForArriveBy(
        reqTime,
        searchParams.searchWindowInSeconds(),
        firstRemovedItinerary == null ? null : firstRemovedItinerary.endTime().toInstant()
      );
    } else {
      return TripSearchMetadata.createForDepartAfter(
        reqTime,
        searchParams.searchWindowInSeconds(),
        firstRemovedItinerary == null ? null : firstRemovedItinerary.startTime().toInstant()
      );
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
