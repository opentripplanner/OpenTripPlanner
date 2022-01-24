package org.opentripplanner.routing.algorithm.mapping;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.model.plan.pagecursor.PageCursor;
import org.opentripplanner.model.plan.pagecursor.PageCursorFactory;
import org.opentripplanner.model.plan.pagecursor.PageType;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.api.response.TripSearchMetadata;
import org.opentripplanner.routing.framework.DebugTimingAggregator;
import org.opentripplanner.transit.raptor.api.request.SearchParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutingResponseMapper {
    private static final Logger LOG = LoggerFactory.getLogger(RoutingResponseMapper.class);

    public static RoutingResponse map(
            RoutingRequest request,
            ZonedDateTime startOfTimeTransit,
            SearchParams searchParams,
            Itinerary firstRemovedItinerary,
            List<Itinerary> itineraries,
            Set<RoutingError> routingErrors,
            DebugTimingAggregator debugTimingAggregator
    ) {
        // Create response
        var tripPlan = TripPlanMapper.mapTripPlan(request, itineraries);

        var factory= mapIntoPageCursorFactory(
                request.getItinerariesSortOrder(),
                startOfTimeTransit,
                searchParams,
                firstRemovedItinerary,
                request.pageCursor == null ? null : request.pageCursor.type
        );

        PageCursor nextPageCursor = factory.nextPageCursor();
        PageCursor prevPageCursor = factory.previousPageCursor();

        LOG.debug("PageCursor current  : " + request.pageCursor);
        LOG.debug("PageCursor previous : " + prevPageCursor);
        LOG.debug("PageCursor next ... : " + nextPageCursor);

        var metadata = createTripSearchMetadata(
                request, searchParams, firstRemovedItinerary
        );

        return new RoutingResponse(
                tripPlan,
                prevPageCursor,
                nextPageCursor,
                metadata,
                new ArrayList<>(routingErrors),
                debugTimingAggregator
        );
    }

    @Nullable
    private static TripSearchMetadata createTripSearchMetadata(
            RoutingRequest request,
            SearchParams searchParams,
            Itinerary firstRemovedItinerary
    ) {
        if(searchParams == null) { return null; }

        Instant reqTime = request.getDateTime();

        if (request.arriveBy) {
            return TripSearchMetadata.createForArriveBy(
                    reqTime,
                    searchParams.searchWindowInSeconds(),
                    firstRemovedItinerary == null
                            ? null
                            : firstRemovedItinerary.endTime().toInstant()
            );
        }
        else {
            return TripSearchMetadata.createForDepartAfter(
                    reqTime,
                    searchParams.searchWindowInSeconds(),
                    firstRemovedItinerary == null
                            ? null
                            : firstRemovedItinerary.startTime().toInstant()
            );
        }
    }

    public static PageCursorFactory mapIntoPageCursorFactory(
            SortOrder sortOrder,
            ZonedDateTime startOfTimeTransit,
            SearchParams searchParams,
            Itinerary firstRemovedItinerary,
            @Nullable PageType currentPageType
    ) {
        var factory = new PageCursorFactory(sortOrder);

        if(searchParams != null) {
            if(!searchParams.isSearchWindowSet()) {
                throw new IllegalArgumentException("SearchWindow not set");
            }
            if(!searchParams.isEarliestDepartureTimeSet()) {
                throw new IllegalArgumentException("Earliest departure time not set");
            }

            long startOfTimeSec = startOfTimeTransit.toEpochSecond();
            var edt = Instant.ofEpochSecond(startOfTimeSec + searchParams.earliestDepartureTime());
            var lat = searchParams.isLatestArrivalTimeSet()
                    ? Instant.ofEpochSecond(startOfTimeSec + searchParams.latestArrivalTime())
                    : null;
            var searchWindow = Duration.ofSeconds(searchParams.searchWindowInSeconds());
            factory.withOriginalSearch(currentPageType, edt, lat, searchWindow);
        }

        if(firstRemovedItinerary != null) {
            factory.withRemovedItineraries(
                    firstRemovedItinerary.startTime().toInstant(),
                    firstRemovedItinerary.endTime().toInstant()
            );
        }

        return factory;
    }
}
