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
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PagingSearchWindowAdjuster;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilterChain;
import org.opentripplanner.routing.algorithm.mapping.RoutingRequestToFilterChainMapper;
import org.opentripplanner.routing.algorithm.mapping.RoutingResponseMapper;
import org.opentripplanner.routing.algorithm.raptoradapter.router.AdditionalSearchDays;
import org.opentripplanner.routing.algorithm.raptoradapter.router.FilterTransitWhenDirectModeIsEmpty;
import org.opentripplanner.routing.algorithm.raptoradapter.router.TransitRouter;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.DirectFlexRouter;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.DirectStreetRouter;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.DateMapper;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.framework.DebugTimingAggregator;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.transit.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.transit.raptor.api.request.SearchParams;
import org.opentripplanner.util.OTPFeature;
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
    public final DebugTimingAggregator debugTimingAggregator = new DebugTimingAggregator();
    public final PagingSearchWindowAdjuster pagingSearchWindowAdjuster;

    private final RoutingRequest request;
    private final Router router;
    /**
     * The transit service time-zero normalized for the current search. All transit times are
     * relative to a "time-zero". This enables us to use an integer(small memory footprint). The
     * times are number for seconds past the {@code transitSearchTimeZero}. In the internal model
     * all times are stored relative to the {@link org.opentripplanner.model.calendar.ServiceDate},
     * but to be able to compare trip times for different service days we normalize all times by
     * calculating an offset. Now all times for the selected trip patterns become relative to the
     * {@code transitSearchTimeZero}.
     */
    private final ZonedDateTime transitSearchTimeZero;
    private SearchParams raptorSearchParamsUsed = null;
    private Itinerary firstRemovedItinerary = null;
    private final AdditionalSearchDays additionalSearchDays;

    public RoutingWorker(Router router, RoutingRequest request, ZoneId zoneId) {
        request.applyPageCursor();
        this.request = request;
        this.router = router;
        this.transitSearchTimeZero = DateMapper.asStartOfService(request.getDateTime(), zoneId);
        this.pagingSearchWindowAdjuster = createPagingSearchWindowAdjuster(router.routerConfig);
        this.additionalSearchDays = createAdditionalSearchDays(
                router.routerConfig.raptorTuningParameters(), zoneId, request
        );
    }

    public RoutingResponse route() {
        // If no direct mode is set, then we set one.
        // See {@link FilterTransitWhenDirectModeIsEmpty}
        var emptyDirectModeHandler = new FilterTransitWhenDirectModeIsEmpty(request.modes);

        request.modes.directMode = emptyDirectModeHandler.resolveDirectMode();

        this.debugTimingAggregator.finishedPrecalculating();

        var itineraries = Collections.synchronizedList(new ArrayList<Itinerary>());
        var routingErrors = Collections.synchronizedSet(new HashSet<RoutingError>());

        if (OTPFeature.ParallelRouting.isOn()) {
            try {
                CompletableFuture.allOf(
                        CompletableFuture.runAsync(() -> routeDirectStreet(itineraries, routingErrors)),
                        CompletableFuture.runAsync(() -> routeDirectFlex(itineraries, routingErrors)),
                        CompletableFuture.runAsync(() -> routeTransit(itineraries, routingErrors))
                ).join();
            }
            catch (CompletionException e) {
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

        debugTimingAggregator.finishedRouting();

        // Filter itineraries
        ItineraryListFilterChain filterChain = RoutingRequestToFilterChainMapper.createFilterChain(
            request.getItinerariesSortOrder(),
            request.itineraryFilters,
            request.numItineraries,
            filterOnLatestDepartureTime(),
            emptyDirectModeHandler.removeWalkAllTheWayResults(),
            request.maxNumberOfItinerariesCropHead(),
            it -> firstRemovedItinerary = it
        );

        List<Itinerary> filteredItineraries = filterChain.filter(itineraries);

        routingErrors.addAll(filterChain.getRoutingErrors());

        if(LOG.isDebugEnabled()) {
            LOG.debug(
                    "Return TripPlan with {} filtered itineraries out of {} total.",
                    filteredItineraries.stream().filter(it -> !it.isFlaggedForDeletion()).count(),
                    itineraries.size());
        }

        this.debugTimingAggregator.finishedFiltering();

        // Restore original directMode.
        request.modes.directMode = emptyDirectModeHandler.originalDirectMode();

        // Adjust the search-window for the next search if the current search-window
        // is off (too few or too many results found).
        var searchWindowNextSearch = calculateSearchWindowNextSearch(filteredItineraries);

        return RoutingResponseMapper.map(
                request,
                transitSearchTimeZero,
                raptorSearchParamsUsed,
                searchWindowNextSearch,
                firstRemovedItinerary,
                filteredItineraries,
                routingErrors,
                debugTimingAggregator
        );
    }

    /**
     * Filter itineraries away that depart after the latest-departure-time for depart after
     * search. These itineraries are a result of time-shifting the access leg and is needed for
     * the raptor to prune the results. These itineraries are often not ideal, but if they
     * pareto optimal for the "next" window, they will appear when a "next" search is performed.
     */
    private Instant filterOnLatestDepartureTime() {
        if(!request.arriveBy
            && raptorSearchParamsUsed != null
            && raptorSearchParamsUsed.isSearchWindowSet()
            && raptorSearchParamsUsed.isEarliestDepartureTimeSet()
        ) {
            int ldt = raptorSearchParamsUsed.earliestDepartureTime()
                    + raptorSearchParamsUsed.searchWindowInSeconds();
            return transitSearchTimeZero.plusSeconds(ldt).toInstant();
        }
        return null;
    }

    private void routeDirectStreet(
            List<Itinerary> itineraries,
            Collection<RoutingError> routingErrors
    ) {
        debugTimingAggregator.startedDirectStreetRouter();
        try {
            itineraries.addAll(DirectStreetRouter.route(router, request));
        } catch (RoutingValidationException e) {
            routingErrors.addAll(e.getRoutingErrors());
        } finally {
            debugTimingAggregator.finishedDirectStreetRouter();
        }
    }

    private void routeDirectFlex(
            List<Itinerary> itineraries,
            Collection<RoutingError> routingErrors
    ) {
        if (!OTPFeature.FlexRouting.isOn()) {
            return;
        }

        debugTimingAggregator.startedDirectFlexRouter();
        try {
            itineraries.addAll(DirectFlexRouter.route(router, request, additionalSearchDays));
        } catch (RoutingValidationException e) {
            routingErrors.addAll(e.getRoutingErrors());
        } finally {
            debugTimingAggregator.finishedDirectFlexRouter();
        }
    }

    private void routeTransit(
            List<Itinerary> itineraries,
            Collection<RoutingError> routingErrors
    ) {
        debugTimingAggregator.startedTransitRouting();
        try {
            var transitResults = TransitRouter.route(
                    request,
                    router,
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
    }

    private static AdditionalSearchDays createAdditionalSearchDays(
            RaptorTuningParameters raptorTuningParameters, ZoneId zoneId, RoutingRequest request
    ) {
        var searchDateTime = ZonedDateTime.ofInstant(request.getDateTime(), zoneId);
        var maxWindow = Duration.ofMinutes(
                raptorTuningParameters
                        .dynamicSearchWindowCoefficients()
                        .maxWinTimeMinutes()
        );

        return new AdditionalSearchDays(
                request.arriveBy,
                searchDateTime,
                request.searchWindow,
                maxWindow,
                request.maxJourneyDuration
        );
    }

    private Duration calculateSearchWindowNextSearch(List<Itinerary> itineraries) {
        // No transit search performed
        if(raptorSearchParamsUsed == null) { return null; }

        var sw = Duration.ofSeconds(raptorSearchParamsUsed.searchWindowInSeconds());

        // SearchWindow cropped -> decrease search-window
        if(firstRemovedItinerary != null) {
            Instant swStartTime = searchStartTime().plusSeconds(raptorSearchParamsUsed.earliestDepartureTime());
            boolean cropSWHead = request.doCropSearchWindowAtTail();
            Instant rmItineraryStartTime = firstRemovedItinerary.startTime().toInstant();

            return pagingSearchWindowAdjuster.decreaseSearchWindow(
                    sw, swStartTime, rmItineraryStartTime, cropSWHead
            );
        }
        // (num-of-itineraries found <= numItineraries)  ->  increase or keep search-window
        else {
            int nRequested = request.numItineraries;
            int nFound = (int) itineraries.stream()
                    .filter(it -> !it.isFlaggedForDeletion() && it.hasTransit())
                    .count();

            return pagingSearchWindowAdjuster.increaseOrKeepSearchWindow(sw, nRequested, nFound);
        }
    }

    private Instant searchStartTime() {
        return transitSearchTimeZero.toInstant();
    }

    private PagingSearchWindowAdjuster createPagingSearchWindowAdjuster(RouterConfig routerConfig) {
        var c = routerConfig.raptorTuningParameters().dynamicSearchWindowCoefficients();
        return new PagingSearchWindowAdjuster(
                c.minWinTimeMinutes(),
                c.maxWinTimeMinutes(),
                routerConfig.transitTuningParameters().pagingSearchWindowAdjustments()
        );
    }
}
