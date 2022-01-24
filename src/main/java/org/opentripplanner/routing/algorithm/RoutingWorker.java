package org.opentripplanner.routing.algorithm;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.pagecursor.PageCursor;
import org.opentripplanner.model.plan.pagecursor.PageType;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilterChain;
import org.opentripplanner.routing.algorithm.mapping.RoutingRequestToFilterChainMapper;
import org.opentripplanner.routing.algorithm.mapping.RoutingResponseMapper;
import org.opentripplanner.routing.algorithm.raptor.router.FilterTransitWhenDirectModeIsEmpty;
import org.opentripplanner.routing.algorithm.raptor.router.TransitRouter;
import org.opentripplanner.routing.algorithm.raptor.router.street.DirectFlexRouter;
import org.opentripplanner.routing.algorithm.raptor.router.street.DirectStreetRouter;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.DateMapper;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.framework.DebugTimingAggregator;
import org.opentripplanner.standalone.server.Router;
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

    private final RoutingRequest request;
    private final Router router;
    private FilterTransitWhenDirectModeIsEmpty emptyDirectModeHandler;

    /**
     * Transit service time-zero. Usually midnight before the request dateTime, but NOT on days
     * witch has DST adjustments.
     */
    private final ZonedDateTime searchTransitTimeZero;
    private SearchParams raptorSearchParamsUsed = null;
    private Itinerary firstRemovedItinerary = null;

    public RoutingWorker(Router router, RoutingRequest request, ZoneId zoneId) {
        this.request = request;
        this.router = router;
        this.searchTransitTimeZero = DateMapper.asStartOfService(request.getDateTimeCurrentPage(), zoneId);
    }

    public RoutingResponse route() {
        applyPageCursor();

        // If no direct mode is set, then we set one.
        // See {@link FilterTransitWhenDirectModeIsEmpty}
        this.emptyDirectModeHandler = new FilterTransitWhenDirectModeIsEmpty(request.modes);
        request.modes.directMode = emptyDirectModeHandler.resolveDirectMode();

        this.debugTimingAggregator.finishedPrecalculating();

        var itineraries = Collections.synchronizedList(new ArrayList<Itinerary>());
        var routingErrors = Collections.synchronizedSet(new HashSet<RoutingError>());

        if (OTPFeature.ParallelRouting.isOn()) {
            CompletableFuture.allOf(
                    CompletableFuture.runAsync(() -> routeDirectStreet(itineraries, routingErrors)),
                    CompletableFuture.runAsync(() -> routeDirectFlex(itineraries, routingErrors)),
                    CompletableFuture.runAsync(() -> routeTransit(itineraries, routingErrors))
            ).join();
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

        LOG.debug("Return TripPlan with {} filtered itineraries out of {} total.", filteredItineraries.size(), itineraries.size());

        this.debugTimingAggregator.finishedFiltering();

        // Restore original directMode.
        request.modes.directMode = emptyDirectModeHandler.originalDirectMode();

        return RoutingResponseMapper.map(
                request,
                searchTransitTimeZero,
                raptorSearchParamsUsed,
                firstRemovedItinerary,
                filteredItineraries,
                routingErrors,
                debugTimingAggregator
        );
    }

    /**
     * Adjust the 'dateTime' if the page cursor is set to "goto next page".
     * The date-time is used for many things, for example finding the days to search,
     * but the transit search is using the cursor[if exist], not the date-time.
     *
     *          * The {@code dateTime}, {@code arriveBy} and {@code pageCursor} depend on each other, to
     *          * enforce the integrity of the theses three variables, we set them all with this method.
     *          *
     *          * @param dateTime The earliest-departure-time {@code arriveBy=false} or latest-arrival-time
     *          *                 {@code arriveBy=true} for this search. If {@code null} now is used.
     *          * @param pageCursor The pageCursor will override the dateTime.
     */
    private void applyPageCursor() {
        PageCursor cursor = request.pageCursor;
        if(cursor != null) {
            if(cursor.type == PageType.NEXT_PAGE) {
                request.arriveBy = false;
                request.setDateTime(cursor.earliestDepartureTime);
            }
            else {
                request.setDateTime(
                        request.arriveBy
                                ? cursor.latestArrivalTime
                                : cursor.earliestDepartureTime
                );
            }
            Instant dateTimeCurrentPage = request.getDateTimeCurrentPage();
            request.setDateTime(dateTimeCurrentPage);

            request.modes.directMode = StreetMode.NOT_SET;
            LOG.debug("Request dateTime={} set from pageCursor.", dateTimeCurrentPage);
        }
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
            return searchTransitTimeZero.plusSeconds(ldt).toInstant();
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
            itineraries.addAll(DirectFlexRouter.route(router, request));
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
                    searchTransitTimeZero,
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
}
