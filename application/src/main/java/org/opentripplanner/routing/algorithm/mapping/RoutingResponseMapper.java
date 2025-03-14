package org.opentripplanner.routing.algorithm.mapping;

import static org.opentripplanner.ext.realtimeresolver.RealtimeResolver.populateLegsWithRealtime;

import java.util.List;
import java.util.Set;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.paging.cursor.PageCursor;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.framework.DebugTimingAggregator;
import org.opentripplanner.service.paging.PagingService;
import org.opentripplanner.transit.service.TransitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutingResponseMapper {

  private static final Logger LOG = LoggerFactory.getLogger(RoutingResponseMapper.class);

  public static RoutingResponse map(
    RouteRequest request,
    List<Itinerary> itineraries,
    Set<RoutingError> routingErrors,
    DebugTimingAggregator debugTimingAggregator,
    TransitService transitService,
    PagingService pagingService
  ) {
    // Search is performed without realtime, but we still want to
    // include realtime information in the result
    if (
      request.preferences().transit().ignoreRealtimeUpdates() && OTPFeature.RealtimeResolver.isOn()
    ) {
      itineraries = populateLegsWithRealtime(itineraries, transitService);
    }

    // Create response
    var tripPlan = TripPlanMapper.mapTripPlan(request, itineraries);

    // Paging
    PageCursor nextPageCursor = pagingService.nextPageCursor();
    PageCursor prevPageCursor = pagingService.previousPageCursor();

    if (LOG.isDebugEnabled()) {
      logPagingInformation(request.pageCursor(), prevPageCursor, nextPageCursor, routingErrors);
    }

    var metadata = pagingService.createTripSearchMetadata();

    return new RoutingResponse(
      tripPlan,
      prevPageCursor,
      nextPageCursor,
      metadata,
      List.copyOf(routingErrors),
      debugTimingAggregator
    );
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
