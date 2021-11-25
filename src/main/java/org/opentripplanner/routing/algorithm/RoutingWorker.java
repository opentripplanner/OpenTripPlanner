package org.opentripplanner.routing.algorithm;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilterChain;
import org.opentripplanner.routing.algorithm.mapping.RoutingRequestToFilterChainMapper;
import org.opentripplanner.routing.algorithm.mapping.TripPlanMapper;
import org.opentripplanner.routing.algorithm.raptor.router.FilterTransitWhenDirectModeIsEmpty;
import org.opentripplanner.routing.algorithm.raptor.router.TransitRouter;
import org.opentripplanner.routing.algorithm.raptor.router.street.DirectFlexRouter;
import org.opentripplanner.routing.algorithm.raptor.router.street.DirectStreetRouter;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.api.response.TripSearchMetadata;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.framework.DebugTimingAggregator;
import org.opentripplanner.standalone.server.Router;
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
    private final FilterTransitWhenDirectModeIsEmpty emptyDirectModeHandler;
    private Itinerary firstRemovedItinerary = null;
    private Instant filterOnLatestDepartureTime = null;
    private int searchWindowUsedInSeconds = TransitRouter.NOT_SET;

    public RoutingWorker(RoutingRequest request, Router router) {
        this.debugTimingAggregator.startedCalculating();
        this.request = request;
        this.router = router;
        this.emptyDirectModeHandler = new FilterTransitWhenDirectModeIsEmpty(request.modes);
    }

    public RoutingResponse route() {
        // If no direct mode is set, then we set one.
        // See {@link FilterTransitWhenDirectModeIsEmpty}
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
            request,
            filterOnLatestDepartureTime,
            emptyDirectModeHandler.removeWalkAllTheWayResults(),
            it -> firstRemovedItinerary = it
        );

        List<Itinerary> filteredItineraries = filterChain.filter(itineraries);

        routingErrors.addAll(filterChain.getRoutingErrors());

        LOG.debug("Return TripPlan with {} filtered itineraries out of {} total.", filteredItineraries.size(), itineraries.size());

        this.debugTimingAggregator.finishedFiltering();

        // Restore original directMode.
        request.modes.directMode = emptyDirectModeHandler.originalDirectMode();

        return new RoutingResponse(
            TripPlanMapper.mapTripPlan(request, filteredItineraries),
            createTripSearchMetadata(),
            new ArrayList<>(routingErrors),
            debugTimingAggregator
        );
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
            var transitResults = TransitRouter.route(request, router, debugTimingAggregator);
            filterOnLatestDepartureTime = transitResults.getFilterOnLatestDepartureTime();
            searchWindowUsedInSeconds = transitResults.getSearchWindowUsedInSeconds();
            itineraries.addAll(transitResults.getItineraries());
        } catch (RoutingValidationException e) {
            routingErrors.addAll(e.getRoutingErrors());
        } finally {
            debugTimingAggregator.finishedTransitRouter();
        }
    }

    @Nullable
    private TripSearchMetadata createTripSearchMetadata() {
        if(searchWindowUsedInSeconds == TransitRouter.NOT_SET) { return null; }

        Instant reqTime = Instant.ofEpochSecond(request.dateTime);

        if (request.arriveBy) {
            return TripSearchMetadata.createForArriveBy(
                reqTime,
                searchWindowUsedInSeconds,
                firstRemovedItinerary == null
                    ? null
                    : firstRemovedItinerary.endTime().toInstant()
            );
        }
        else {
            return TripSearchMetadata.createForDepartAfter(
                reqTime,
                searchWindowUsedInSeconds,
                firstRemovedItinerary == null
                    ? null
                    : firstRemovedItinerary.startTime().toInstant()
            );
        }
    }
}
