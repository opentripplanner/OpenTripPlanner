package org.opentripplanner.routing.algorithm.mapping;

import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PageCursor;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TestItineraryBuilder;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.DateMapper;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.api.request.SearchParams;

class PageCursorFactoryTest implements PlanTestConstants {

    static final ZoneId zoneId = UTC;
    static final Instant time = Instant.parse("2020-02-02T12:00:00Z");
    static final ZonedDateTime startOfTime = DateMapper.asStartOfService(LocalDate.ofInstant(time, zoneId), zoneId);

    static final int T12_00 = 12 * 60 * 60;
    static final int T12_30 = (int) (12.5 * 60 * 60);
    static final int T13_00 = 13 * 60 * 60;
    static final int T13_30 = (int) (13.5 * 60 * 60);
    public static final Duration D1H = Duration.ofHours(1);
    public static final Duration D30M = Duration.ofMinutes(30);
    public static final Duration D1H30M = Duration.ofMinutes(90);
    public static final Duration D2H30M = Duration.ofMinutes(150);

    @Test
    public void testDepartAtNoRemovedNextPage() {
        SearchParams searchParams = new RaptorRequestBuilder<TestTripSchedule>()
                .searchParams()
                .earliestDepartureTime(T12_00)
                .searchWindow(D1H)
                .buildSearchParam();


        PageCursor pageCursor = new PageCursorFactory(
                false,
                startOfTime,
                searchParams,
                null,
                false
        ).createNextPageCursor();

        assertEquals(time.plus(D1H), pageCursor.earliestDepartureTime);
        assertNull(pageCursor.latestArrivalTime);
        assertEquals(D1H, pageCursor.searchWindow);
    }

    @Test
    public void testDepartAtNoRemovedPreviousPage() {
        SearchParams searchParams = new RaptorRequestBuilder<TestTripSchedule>()
                .searchParams()
                .earliestDepartureTime(T12_00)
                .searchWindow(D1H)
                .buildSearchParam();


        PageCursor pageCursor = new PageCursorFactory(
                false,
                startOfTime,
                searchParams,
                null,
                false
        ).createPreviousPageCursor();

        assertEquals(time.minus(D1H), pageCursor.earliestDepartureTime);
        assertNull(pageCursor.latestArrivalTime);
        assertEquals(D1H, pageCursor.searchWindow);
    }

    @Test
    public void testDepartAtRemovedInsideNextPage() {
        SearchParams searchParams = new RaptorRequestBuilder<TestTripSchedule>()
                .searchParams()
                .earliestDepartureTime(T12_00)
                .searchWindow(D1H)
                .buildSearchParam();

        Itinerary itinerary = TestItineraryBuilder.newItinerary(A, T12_30).bus(1, T12_30, T13_30, B).build();

        PageCursor pageCursor = new PageCursorFactory(
                false,
                startOfTime,
                searchParams,
                itinerary,
                false
        ).createNextPageCursor();

        assertEquals(time.plus(D30M), pageCursor.earliestDepartureTime);
        assertNull(pageCursor.latestArrivalTime);
        assertEquals(D1H, pageCursor.searchWindow);
    }

    @Test
    public void testDepartAtRemovedInsidePreviousPage() {
        SearchParams searchParams = new RaptorRequestBuilder<TestTripSchedule>()
                .searchParams()
                .earliestDepartureTime(T12_00)
                .searchWindow(D1H)
                .buildSearchParam();

        Itinerary itinerary = TestItineraryBuilder.newItinerary(A, T12_30).bus(1, T12_30, T13_30, B).build();

        PageCursor pageCursor = new PageCursorFactory(
                false,
                startOfTime,
                searchParams,
                itinerary,
                false
        ).createPreviousPageCursor();

        assertEquals(time.minus(D1H), pageCursor.earliestDepartureTime);
        assertNull(pageCursor.latestArrivalTime);
        assertEquals(D1H, pageCursor.searchWindow);
    }

    @Test
    public void testDepartAtReversedNoRemovedNextPage() {
        SearchParams searchParams = new RaptorRequestBuilder<TestTripSchedule>()
                .searchParams()
                .earliestDepartureTime(T12_00)
                .searchWindow(D1H)
                .buildSearchParam();


        PageCursor pageCursor = new PageCursorFactory(
                false,
                startOfTime,
                searchParams,
                null,
                true
        ).createNextPageCursor();

        assertEquals(time.plus(D1H), pageCursor.earliestDepartureTime);
        assertNull(pageCursor.latestArrivalTime);
        assertEquals(D1H, pageCursor.searchWindow);
    }

    @Test
    public void testDepartAtReversedNoRemovedPreviousPage() {
        SearchParams searchParams = new RaptorRequestBuilder<TestTripSchedule>()
                .searchParams()
                .earliestDepartureTime(T12_00)
                .searchWindow(D1H)
                .buildSearchParam();


        PageCursor pageCursor = new PageCursorFactory(
                false,
                startOfTime,
                searchParams,
                null,
                true
        ).createPreviousPageCursor();

        assertEquals(time.minus(D1H), pageCursor.earliestDepartureTime);
        assertNull(pageCursor.latestArrivalTime);
        assertEquals(D1H, pageCursor.searchWindow);
    }

    @Test
    public void testDepartAtReversedRemovedInsideNextPage() {
        SearchParams searchParams = new RaptorRequestBuilder<TestTripSchedule>()
                .searchParams()
                .earliestDepartureTime(T12_00)
                .searchWindow(D1H)
                .buildSearchParam();

        Itinerary itinerary = TestItineraryBuilder.newItinerary(A, T12_30).bus(1, T12_30, T13_30, B).build();

        PageCursor pageCursor = new PageCursorFactory(
                false,
                startOfTime,
                searchParams,
                itinerary,
                true
        ).createNextPageCursor();

        assertEquals(time.plus(D1H), pageCursor.earliestDepartureTime);
        assertNull(pageCursor.latestArrivalTime);
        assertEquals(D1H, pageCursor.searchWindow);
    }

    @Test
    public void testDepartAtReversedRemovedInsidePreviousPage() {
        SearchParams searchParams = new RaptorRequestBuilder<TestTripSchedule>()
                .searchParams()
                .earliestDepartureTime(T12_00)
                .searchWindow(D1H)
                .buildSearchParam();

        Itinerary itinerary = TestItineraryBuilder.newItinerary(A, T12_30).bus(1, T12_30, T13_30, B).build();

        PageCursor pageCursor = new PageCursorFactory(
                false,
                startOfTime,
                searchParams,
                itinerary,
                true
        ).createPreviousPageCursor();

        assertEquals(time.minus(D1H), pageCursor.earliestDepartureTime);
        assertEquals(time.plus(D1H30M), pageCursor.latestArrivalTime);
        assertEquals(D1H, pageCursor.searchWindow);
    }

    @Test
    public void testArriveByNoRemovedNextPage() {
        SearchParams searchParams = new RaptorRequestBuilder<TestTripSchedule>()
                .searchParams()
                .earliestDepartureTime(T12_00)
                .latestArrivalTime(T13_30)
                .searchWindow(D1H)
                .buildSearchParam();


        PageCursor pageCursor = new PageCursorFactory(
                true,
                startOfTime,
                searchParams,
                null,
                false
        ).createNextPageCursor();

        assertEquals(time.plus(D1H), pageCursor.earliestDepartureTime);
        assertEquals(time.plus(D2H30M), pageCursor.latestArrivalTime);
        assertEquals(D1H, pageCursor.searchWindow);
    }

    @Test
    public void testArriveByNoRemovedPreviousPage() {
        SearchParams searchParams = new RaptorRequestBuilder<TestTripSchedule>()
                .searchParams()
                .earliestDepartureTime(T12_00)
                .latestArrivalTime(T13_30)
                .searchWindow(D1H)
                .buildSearchParam();


        PageCursor pageCursor = new PageCursorFactory(
                true,
                startOfTime,
                searchParams,
                null,
                false
        ).createPreviousPageCursor();

        assertEquals(time.minus(D1H), pageCursor.earliestDepartureTime);
        assertEquals(time.plus(D30M), pageCursor.latestArrivalTime);
        assertEquals(D1H, pageCursor.searchWindow);
    }

    @Test
    public void testArriveByRemovedInsideNextPage() {
        SearchParams searchParams = new RaptorRequestBuilder<TestTripSchedule>()
                .searchParams()
                .earliestDepartureTime(T12_00)
                .latestArrivalTime(T13_30)
                .searchWindow(D1H)
                .buildSearchParam();

        Itinerary itinerary = TestItineraryBuilder.newItinerary(A, T12_30).bus(1, T12_30, T13_00, B).build();

        PageCursor pageCursor = new PageCursorFactory(
                true,
                startOfTime,
                searchParams,
                itinerary,
                false
        ).createNextPageCursor();

        assertEquals(time.plus(D1H), pageCursor.earliestDepartureTime);
        assertEquals(time.plus(D2H30M), pageCursor.latestArrivalTime);
        assertEquals(D1H, pageCursor.searchWindow);
    }

    @Test
    public void testArriveByRemovedInsidePreviousPage() {
        SearchParams searchParams = new RaptorRequestBuilder<TestTripSchedule>()
                .searchParams()
                .earliestDepartureTime(T12_00)
                .latestArrivalTime(T13_30)
                .searchWindow(D1H)
                .buildSearchParam();

        Itinerary itinerary = TestItineraryBuilder.newItinerary(A, T12_30).bus(1, T12_30, T13_00, B).build();

        PageCursor pageCursor = new PageCursorFactory(
                true,
                startOfTime,
                searchParams,
                itinerary,
                false
        ).createPreviousPageCursor();

        assertEquals(time.minus(D30M), pageCursor.earliestDepartureTime);
        assertEquals(time.plus(D1H), pageCursor.latestArrivalTime);
        assertEquals(D1H, pageCursor.searchWindow);
    }

    @Test
    public void testArriveByReversedNoRemovedNextPage() {
        SearchParams searchParams = new RaptorRequestBuilder<TestTripSchedule>()
                .searchParams()
                .earliestDepartureTime(T12_00)
                .latestArrivalTime(T13_30)
                .searchWindow(D1H)
                .buildSearchParam();


        PageCursor pageCursor = new PageCursorFactory(
                true,
                startOfTime,
                searchParams,
                null,
                true
        ).createNextPageCursor();

        assertEquals(time.plus(D1H), pageCursor.earliestDepartureTime);
        assertEquals(time.plus(D2H30M), pageCursor.latestArrivalTime);
        assertEquals(D1H, pageCursor.searchWindow);
    }

    @Test
    public void testArriveByReversedNoRemovedPreviousPage() {
        SearchParams searchParams = new RaptorRequestBuilder<TestTripSchedule>()
                .searchParams()
                .earliestDepartureTime(T12_00)
                .latestArrivalTime(T13_30)
                .searchWindow(D1H)
                .buildSearchParam();


        PageCursor pageCursor = new PageCursorFactory(
                true,
                startOfTime,
                searchParams,
                null,
                true
        ).createPreviousPageCursor();

        assertEquals(time.minus(D1H), pageCursor.earliestDepartureTime);
        assertEquals(time.plus(D30M), pageCursor.latestArrivalTime);
        assertEquals(D1H, pageCursor.searchWindow);
    }

    @Test
    public void testArriveByReversedRemovedInsideNextPage() {
        SearchParams searchParams = new RaptorRequestBuilder<TestTripSchedule>()
                .searchParams()
                .earliestDepartureTime(T12_00)
                .latestArrivalTime(T13_30)
                .searchWindow(D1H)
                .buildSearchParam();

        Itinerary itinerary = TestItineraryBuilder.newItinerary(A, T12_30).bus(1, T12_30, T13_00, B).build();

        PageCursor pageCursor = new PageCursorFactory(
                true,
                startOfTime,
                searchParams,
                itinerary,
                true
        ).createNextPageCursor();

        assertEquals(time.plus(D30M), pageCursor.earliestDepartureTime);
        assertEquals(time.plus(D1H), pageCursor.latestArrivalTime);
        assertEquals(D1H, pageCursor.searchWindow);
    }

    @Test
    public void testArriveByReversedRemovedInsidePreviousPage() {
        SearchParams searchParams = new RaptorRequestBuilder<TestTripSchedule>()
                .searchParams()
                .earliestDepartureTime(T12_00)
                .latestArrivalTime(T13_30)
                .searchWindow(D1H)
                .buildSearchParam();

        Itinerary itinerary = TestItineraryBuilder.newItinerary(A, T12_30).bus(1, T12_30, T13_00, B).build();

        PageCursor pageCursor = new PageCursorFactory(
                true,
                startOfTime,
                searchParams,
                itinerary,
                true
        ).createPreviousPageCursor();

        assertEquals(time.minus(D1H), pageCursor.earliestDepartureTime);
        assertEquals(time.plus(D30M), pageCursor.latestArrivalTime);
        assertEquals(D1H, pageCursor.searchWindow);
    }
}