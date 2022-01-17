package org.opentripplanner.routing.algorithm.mapping;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.model.plan.Itinerary;
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
            DebugTimingAggregator debugTimingAggregator,
            boolean reverseFilteringDirection
    ) {
        // Create response
        var tripPlan = TripPlanMapper.mapTripPlan(request, itineraries);

        var pageCursorFactory = new PageCursorFactory(
                request.arriveBy,
                startOfTimeTransit,
                searchParams,
                firstRemovedItinerary,
                reverseFilteringDirection
        );

        var previousPageCursor = pageCursorFactory.createPreviousPageCursor();
        var nextPageCursor = pageCursorFactory.createNextPageCursor();

        LOG.debug("PageCursor in: " + request.pageCursor);
        LOG.debug("PageCursor out(previous) : " + previousPageCursor);
        LOG.debug("PageCursor out(next) : " + nextPageCursor);

        var metadata = createTripSearchMetadata(
                request, searchParams, firstRemovedItinerary
        );

        return new RoutingResponse(
                tripPlan,
                previousPageCursor,
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

        Instant reqTime = request.getDateTimeCurrentPage();

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
}
