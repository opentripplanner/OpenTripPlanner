package org.opentripplanner.model.plan.pagecursor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.util.time.TimeUtils;

@SuppressWarnings("ConstantConditions")
class PageCursorFactoryTest implements PlanTestConstants {
    static final boolean ARRIVE_BY = true;
    static final boolean DEPART_AFTER = false;

    static final boolean REVERSE = true;
    static final boolean FORWARDS = false;

    static final Instant TIME_ZERO = Instant.parse("2020-02-02T00:00:00Z");

    public static final Duration D1H = Duration.ofHours(1);

    private static final Instant T10_30 = time("10:30");
    private static final Instant T11_00 = time("11:00");
    private static final Instant T11_30 = time("11:30");
    private static final Instant T12_00 = time("12:00");
    private static final Instant T12_30 = time("12:30");
    private static final Instant T13_00 = time("13:00");
    private static final Instant T13_30 = time("13:30");
    private static final Instant T14_30 = time("14:30");


    @Test
    public void departAfter() {
        var factory = new PageCursorFactory(DEPART_AFTER, FORWARDS)
                .withOriginalSearch(T12_00, null, D1H);

        var nextPage = factory.nextPageCursor();
        assetPageCursor(nextPage, T13_00, null, D1H, FORWARDS);

        var prevPage = factory.previousPageCursor();
        assetPageCursor(prevPage, T11_00, null, D1H, REVERSE);
    }

    @Test
    public void departAfterCropSearchWindow() {
        var factory = new PageCursorFactory(DEPART_AFTER, FORWARDS)
                .withOriginalSearch(T12_00, null, D1H)
                .withRemovedItineraries(T12_30, T13_30);

        var nextPage = factory.nextPageCursor();
        assetPageCursor(nextPage, T12_30, null, D1H, FORWARDS);

        var prevPage = factory.previousPageCursor();
        assetPageCursor(prevPage, T11_00, null, D1H, REVERSE);
    }

    @Test
    public void departAfterReversedFilter() {
        var factory = new PageCursorFactory(DEPART_AFTER, REVERSE)
                .withOriginalSearch(T12_00, null, D1H);

        var nextPage = factory.nextPageCursor();
        assetPageCursor(nextPage, T13_00, null, D1H, FORWARDS);

        var prevPage = factory.previousPageCursor();
        assetPageCursor(prevPage, T11_00, null, D1H, REVERSE);
    }

    @Test
    public void departAfterCropSearchWindowReversedFilter() {
        var factory = new PageCursorFactory(DEPART_AFTER, REVERSE)
                .withOriginalSearch(T12_00, null, D1H)
                .withRemovedItineraries(T12_30, T13_30);

        var nextPage = factory.nextPageCursor();
        assetPageCursor(nextPage, T13_00, null, D1H, FORWARDS);

        var prevPage = factory.previousPageCursor();
        assetPageCursor(prevPage, T11_00, T13_30, D1H, REVERSE);
    }

    @Test
    public void arriveBy() {
        var factory = new PageCursorFactory(ARRIVE_BY, FORWARDS)
                .withOriginalSearch(T12_00, T13_30, D1H);

        var nextPage = factory.nextPageCursor();
        assetPageCursor(nextPage, T13_00, T14_30, D1H, REVERSE);

        var prevPage = factory.previousPageCursor();
        assetPageCursor(prevPage, T11_00, T12_30, D1H, FORWARDS);
    }

    @Test
    public void arriveByReversedFilter() {
        var factory = new PageCursorFactory(ARRIVE_BY, REVERSE)
                .withOriginalSearch(T12_00, T13_30, D1H);

        var nextPage = factory.nextPageCursor();
        assetPageCursor(nextPage, T13_00, T14_30, D1H, REVERSE);

        var prevPage = factory.previousPageCursor();
        assetPageCursor(prevPage, T11_00, T12_30, D1H, FORWARDS);
    }

    @Test
    public void arriveByCropSearchWindow() {
        var factory = new PageCursorFactory(ARRIVE_BY, FORWARDS)
                .withOriginalSearch(T12_00, T13_30, D1H)
                .withRemovedItineraries(T12_30, T13_00);

        var nextPage = factory.nextPageCursor();
        assetPageCursor(nextPage, T13_00, T14_30, D1H, REVERSE);

        var prevPage = factory.previousPageCursor();
        assetPageCursor(prevPage, T11_30, T13_00, D1H, FORWARDS);
    }

    @Test
    public void arriveByCropSearchWindowReversedFilter() {
        var factory = new PageCursorFactory(ARRIVE_BY, REVERSE)
                .withOriginalSearch(T12_00, T13_30, D1H)
                .withRemovedItineraries(T12_30, T13_00);

        var nextPage = factory.nextPageCursor();
        assetPageCursor(nextPage, T12_30, T13_00, D1H, REVERSE);

        var prevPage = factory.previousPageCursor();
        assetPageCursor(prevPage, T11_00, T12_30, D1H, FORWARDS);
    }

    private void assetPageCursor(
            PageCursor pageCursor,
            Instant expEdt,
            Instant expLat,
            Duration expSearchWindow,
            boolean expReverseFilter
    ) {
        assertEquals(expEdt, pageCursor.earliestDepartureTime);
        assertEquals(expLat, pageCursor.latestArrivalTime);
        assertEquals(expSearchWindow, pageCursor.searchWindow);
        assertEquals(expReverseFilter, pageCursor.reverseFilteringDirection);
    }

    private static Instant time(String input) {
        return TIME_ZERO.plusSeconds(TimeUtils.time(input));
    }
}