package org.opentripplanner.routing.algorithm;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import javax.annotation.Nullable;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.grouppriority.TransitGroupPriorityItineraryDecorator;
import org.opentripplanner.model.plan.paging.cursor.PageCursorInput;
import org.opentripplanner.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.raptor.api.request.SearchParams;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilterChain;
import org.opentripplanner.routing.algorithm.mapping.PagingServiceFactory;
import org.opentripplanner.routing.algorithm.mapping.RouteRequestToFilterChainMapper;
import org.opentripplanner.routing.algorithm.mapping.RoutingResponseMapper;
import org.opentripplanner.routing.algorithm.raptoradapter.router.AdditionalSearchDays;
import org.opentripplanner.routing.algorithm.raptoradapter.router.FilterTransitWhenDirectModeIsEmpty;
import org.opentripplanner.routing.algorithm.raptoradapter.router.TransitRouter;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.DirectFlexRouter;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.DirectStreetRouter;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.framework.DebugTimingAggregator;
import org.opentripplanner.service.paging.PagingService;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.model.network.grouppriority.TransitGroupPriorityService;
import org.opentripplanner.utils.time.ServiceDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Does a complete transit search, including access and egress legs.
 * <p>
 * This class has a request scope, hence the "Worker" name.
 */
public class RoutingWorker {

  private static final Logger LOG = LoggerFactory.getLogger(RoutingWorker.class);

  /** An object that accumulates profiling and debugging info for inclusion in the response. */
  private final DebugTimingAggregator debugTimingAggregator;

  private final RouteRequest request;
  private final OtpServerRequestContext serverContext;
  /**
   * The transit service time-zero normalized for the current search. All transit times are relative
   * to a "time-zero". This enables us to use an integer(small memory footprint). The times are
   * number for seconds past the {@code transitSearchTimeZero}. In the internal model all times are
   * stored relative to the {@link java.time.LocalDate}, but to be able
   * to compare trip times for different service days we normalize all times by calculating an
   * offset. Now all times for the selected trip patterns become relative to the {@code
   * transitSearchTimeZero}.
   */
  private final ZonedDateTime transitSearchTimeZero;
  private final AdditionalSearchDays additionalSearchDays;
  private final TransitGroupPriorityService transitGroupPriorityService;
  private SearchParams raptorSearchParamsUsed = null;
  private PageCursorInput pageCursorInput = null;

  public RoutingWorker(OtpServerRequestContext serverContext, RouteRequest request, ZoneId zoneId) {
    request.applyPageCursor();
    this.request = request;
    this.serverContext = serverContext;
    this.debugTimingAggregator = new DebugTimingAggregator(
      serverContext.meterRegistry(),
      request.preferences().system().tags()
    );
    this.transitSearchTimeZero = ServiceDateUtils.asStartOfService(request.dateTime(), zoneId);
    this.additionalSearchDays = createAdditionalSearchDays(
      serverContext.raptorTuningParameters(),
      zoneId,
      request
    );
    this.transitGroupPriorityService = TransitGroupPriorityService.of(
      request.preferences().transit().relaxTransitGroupPriority(),
      request.journey().transit().priorityGroupsByAgency(),
      request.journey().transit().priorityGroupsGlobal()
    );
  }

  public RoutingResponse route() {
    OTPRequestTimeoutException.checkForTimeout();

    // If no direct mode is set, then we set one.
    // See {@link FilterTransitWhenDirectModeIsEmpty}
    var emptyDirectModeHandler = new FilterTransitWhenDirectModeIsEmpty(
      request.journey().direct().mode()
    );

    request.journey().direct().setMode(emptyDirectModeHandler.resolveDirectMode());

    this.debugTimingAggregator.finishedPrecalculating();

    var itineraries = Collections.synchronizedList(new ArrayList<Itinerary>());
    var routingErrors = Collections.synchronizedSet(new HashSet<RoutingError>());

    if (OTPFeature.ParallelRouting.isOn()) {
      // TODO: This is not using {@link OtpRequestThreadFactory} which means we do not get
      //       log-trace-parameters-propagation and graceful timeout handling here.
      try {
        CompletableFuture.allOf(
          CompletableFuture.runAsync(() -> routeDirectStreet(itineraries, routingErrors)),
          CompletableFuture.runAsync(() -> routeDirectFlex(itineraries, routingErrors)),
          CompletableFuture.runAsync(() -> routeTransit(itineraries, routingErrors))
        ).join();
      } catch (CompletionException e) {
        RoutingValidationException.unwrapAndRethrowCompletionException(e);
      }
    } else {
      // Direct street routing
      routeDirectStreet(itineraries, routingErrors);

      // Direct flex routing
      routeDirectFlex(itineraries, routingErrors);

      // Transit routing
      routeTransit(itineraries, routingErrors);
    }

    // Set C2 value for Street and FLEX if transit-group-priority is used
    new TransitGroupPriorityItineraryDecorator(transitGroupPriorityService).decorate(itineraries);

    debugTimingAggregator.finishedRouting();

    // Filter itineraries
    List<Itinerary> filteredItineraries;
    {
      boolean removeWalkAllTheWayResultsFromDirectFlex =
        request.journey().direct().mode() == StreetMode.FLEXIBLE;

      ItineraryListFilterChain filterChain = RouteRequestToFilterChainMapper.createFilterChain(
        request,
        serverContext,
        earliestDepartureTimeUsed(),
        searchWindowUsed(),
        emptyDirectModeHandler.removeWalkAllTheWayResults() ||
        removeWalkAllTheWayResultsFromDirectFlex,
        it -> pageCursorInput = it
      );

      filteredItineraries = filterChain.filter(itineraries);
      routingErrors.addAll(filterChain.getRoutingErrors());
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug(
        "Return TripPlan with {} filtered itineraries out of {} total.",
        filteredItineraries.stream().filter(it -> !it.isFlaggedForDeletion()).count(),
        itineraries.size()
      );
    }

    this.debugTimingAggregator.finishedFiltering();

    // Restore original directMode.
    request.journey().direct().setMode(emptyDirectModeHandler.originalDirectMode());

    // Adjust the search-window for the next search if the current search-window
    // is off (too few or too many results found).

    var pagingService = createPagingService(itineraries);

    return RoutingResponseMapper.map(
      request,
      raptorSearchParamsUsed,
      filteredItineraries,
      routingErrors,
      debugTimingAggregator,
      serverContext.transitService(),
      pagingService
    );
  }

  private static AdditionalSearchDays createAdditionalSearchDays(
    RaptorTuningParameters raptorTuningParameters,
    ZoneId zoneId,
    RouteRequest request
  ) {
    var searchDateTime = ZonedDateTime.ofInstant(request.dateTime(), zoneId);
    var maxWindow = raptorTuningParameters.dynamicSearchWindowCoefficients().maxWindow();

    return new AdditionalSearchDays(
      request.arriveBy(),
      searchDateTime,
      request.searchWindow(),
      maxWindow,
      request.preferences().system().maxJourneyDuration()
    );
  }

  /**
   * Calculate the earliest-departure-time used in the transit search.
   * This method returns {@code null} if no transit search is performed.
   */
  @Nullable
  private Instant earliestDepartureTimeUsed() {
    if (raptorSearchParamsUsed == null) {
      return null;
    }
    if (!raptorSearchParamsUsed.isEarliestDepartureTimeSet()) {
      return null;
    }
    return transitSearchTimeZero
      .plusSeconds(raptorSearchParamsUsed.earliestDepartureTime())
      .toInstant();
  }

  /**
   * Calculate the search-window earliest-departure-time used in the transit search.
   * This method returns {@code null} if no transit search is performed.
   */
  @Nullable
  private Duration searchWindowUsed() {
    return raptorSearchParamsUsed == null
      ? null
      : Duration.ofSeconds(raptorSearchParamsUsed.searchWindowInSeconds());
  }

  private List<Itinerary> routeDirectStreet(
    List<Itinerary> itineraries,
    Collection<RoutingError> routingErrors
  ) {
    // TODO: Add support for via search to the direct-street search and remove this.
    //       The direct search is used to prune away silly transit results and it
    //       would be nice to also support via as a feature in the direct-street
    //       search.
    if (request.isViaSearch()) {
      return null;
    }

    debugTimingAggregator.startedDirectStreetRouter();
    try {
      itineraries.addAll(DirectStreetRouter.route(serverContext, request));
    } catch (RoutingValidationException e) {
      routingErrors.addAll(e.getRoutingErrors());
    } finally {
      debugTimingAggregator.finishedDirectStreetRouter();
    }
    return null;
  }

  private Void routeDirectFlex(
    List<Itinerary> itineraries,
    Collection<RoutingError> routingErrors
  ) {
    if (!OTPFeature.FlexRouting.isOn()) {
      return null;
    }

    debugTimingAggregator.startedDirectFlexRouter();
    try {
      itineraries.addAll(DirectFlexRouter.route(serverContext, request, additionalSearchDays));
    } catch (RoutingValidationException e) {
      routingErrors.addAll(e.getRoutingErrors());
    } finally {
      debugTimingAggregator.finishedDirectFlexRouter();
    }
    return null;
  }

  private Void routeTransit(List<Itinerary> itineraries, Collection<RoutingError> routingErrors) {
    debugTimingAggregator.startedTransitRouting();
    try {
      var transitResults = TransitRouter.route(
        request,
        serverContext,
        transitGroupPriorityService,
        transitSearchTimeZero,
        additionalSearchDays,
        debugTimingAggregator
      );
      raptorSearchParamsUsed = transitResults.getSearchParams();
      itineraries.addAll(transitResults.getItineraries());
    } catch (RoutingValidationException e) {
      routingErrors.addAll(e.getRoutingErrors());
    } finally {
      debugTimingAggregator.finishedTransitRouter();
    }
    return null;
  }

  private Instant searchStartTime() {
    return transitSearchTimeZero.toInstant();
  }

  private PagingService createPagingService(List<Itinerary> itineraries) {
    return PagingServiceFactory.createPagingService(
      searchStartTime(),
      serverContext.transitTuningParameters(),
      serverContext.raptorTuningParameters(),
      request,
      raptorSearchParamsUsed,
      pageCursorInput,
      itineraries
    );
  }
}
