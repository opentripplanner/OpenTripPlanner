package org.opentripplanner.routing.algorithm.mapping;

import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.model.plan.PlanTestConstants.A;
import static org.opentripplanner.model.plan.PlanTestConstants.B;

import java.time.Duration;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.TestItineraryBuilder;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.api.request.SearchParams;
import org.opentripplanner.util.time.TimeUtils;

public class RoutingResponseMapperTest {
    static final ZonedDateTime TRANSIT_TIME_ZERO = TestItineraryBuilder.SERVICE_DAY
            .atStartOfDay(UTC);

    static final int T12_00 = TimeUtils.hm2time(12, 0);
    static final int T12_30 = TimeUtils.hm2time(12, 30);
    static final int T13_00 = TimeUtils.hm2time(13, 0);
    static final int T13_30 = TimeUtils.hm2time(13, 30);
    
    static final Duration D1H = Duration.ofHours(1);

    private static final SearchParams SEARCH_PARAMS = new RaptorRequestBuilder<TestTripSchedule>()
            .searchParams()
            .earliestDepartureTime(T12_00)
            .latestArrivalTime(T13_30)
            .searchWindow(D1H)
            .buildSearchParam();

    private static final Itinerary REMOVED_ITINERARY = TestItineraryBuilder.newItinerary(A, T12_30)
            .bus(1, T12_30, T13_00, B)
            .build();


    @Test
    void mapIntoPageCursorFactoryNoTransitSearchParams() {
        var factory = RoutingResponseMapper.mapIntoPageCursorFactory(
                false,
                TRANSIT_TIME_ZERO,
                null,
                null,
                false
        );

        assertNull(factory.nextPageCursor());
        assertNull(factory.previousPageCursor());
    }

    
    @Test
    void mapIntoPageCursorFactoryWithSearchParamsNoItineraryRemoved() {

        var factory = RoutingResponseMapper.mapIntoPageCursorFactory(
                false,
                TRANSIT_TIME_ZERO,
                SEARCH_PARAMS,
                null,
                false
        );

        // There is no way to access the internals of the factory, so we use the toString()
        assertEquals(
                "PageCursorFactory{"
                        + "arriveBy: false, "
                        + "reverseFilteringDirection: false, "
                        + "original: Search{edt: 2020-02-02T12:00:00Z, lat: 2020-02-02T13:30:00Z}, "
                        + "originalSearchWindow: 1h, "
                        + "swCropped: false"
                        + "}",
                factory.toString()
        );
    }

    @Test
    public void testArriveByReversedRemovedInsidePreviousPage() {

        var factory = RoutingResponseMapper.mapIntoPageCursorFactory(
                true,
                TRANSIT_TIME_ZERO,
                SEARCH_PARAMS,
                REMOVED_ITINERARY,
                true
        );

        // There is no way to access the internals of the factory, so we use the toString()
        assertEquals(
                "PageCursorFactory{"
                        + "arriveBy: true, "
                        + "reverseFilteringDirection: true, "
                        + "original: Search{edt: 2020-02-02T12:00:00Z, lat: 2020-02-02T13:30:00Z}, "
                        + "originalSearchWindow: 1h, "
                        + "swCropped: true, "
                        + "firstRemovedItineraryStartTime: 2020-02-02T12:30:00Z, "
                        + "firstRemovedItineraryEndTime: 2020-02-02T13:00:00Z"
                        + "}",
                factory.toString()
        );
    }
}