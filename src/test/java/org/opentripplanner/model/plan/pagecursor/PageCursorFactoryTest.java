package org.opentripplanner.model.plan.pagecursor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.SortOrder.STREET_AND_ARRIVAL_TIME;
import static org.opentripplanner.model.plan.SortOrder.STREET_AND_DEPARTURE_TIME;
import static org.opentripplanner.model.plan.pagecursor.PageType.NEXT_PAGE;
import static org.opentripplanner.model.plan.pagecursor.PageType.PREVIOUS_PAGE;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.util.time.TimeUtils;

@SuppressWarnings("ConstantConditions")
class PageCursorFactoryTest implements PlanTestConstants {

    static final Instant TIME_ZERO = Instant.parse("2020-02-02T00:00:00Z");

    public static final Duration D1H = Duration.ofHours(1);

    private static final Instant T11_00 = time("11:00");
    private static final Instant T11_30 = time("11:30");
    private static final Instant T12_00 = time("12:00");
    private static final Instant T12_30 = time("12:30");
    private static final Instant T13_00 = time("13:00");
    private static final Instant T13_30 = time("13:30");
    private static final Instant T14_30 = time("14:30");


    @Test
    public void sortByArrival() {
        var factory = new PageCursorFactory(STREET_AND_ARRIVAL_TIME)
                .withOriginalSearch(null, T12_00, null, D1H);

        var nextPage = factory.nextPageCursor();
        assetPageCursor(nextPage, T13_00, null, D1H, NEXT_PAGE);

        var prevPage = factory.previousPageCursor();
        assetPageCursor(prevPage, T11_00, null, D1H, PREVIOUS_PAGE);
    }

    @Test
    public void sortByArrivalCropSearchWindow() {
        var factory = new PageCursorFactory(STREET_AND_ARRIVAL_TIME)
                .withOriginalSearch(NEXT_PAGE, T12_00, null, D1H)
                .withRemovedItineraries(T12_30, T13_30);

        var nextPage = factory.nextPageCursor();
        assetPageCursor(nextPage, T12_30, null, D1H, NEXT_PAGE);

        var prevPage = factory.previousPageCursor();
        assetPageCursor(prevPage, T11_00, null, D1H, PREVIOUS_PAGE);
    }

    @Test
    public void sortByArrivalPreviousPage() {
        var factory = new PageCursorFactory(STREET_AND_ARRIVAL_TIME)
                .withOriginalSearch(PREVIOUS_PAGE, T12_00, null, D1H);

        var nextPage = factory.nextPageCursor();
        assetPageCursor(nextPage, T13_00, null, D1H, NEXT_PAGE);

        var prevPage = factory.previousPageCursor();
        assetPageCursor(prevPage, T11_00, null, D1H, PREVIOUS_PAGE);
    }

    @Test
    public void sortByArrivalCropSearchWindowPreviousPage() {
        var factory = new PageCursorFactory(STREET_AND_ARRIVAL_TIME)
                .withOriginalSearch(PREVIOUS_PAGE, T12_00, null, D1H)
                .withRemovedItineraries(T12_30, T13_30);

        var nextPage = factory.nextPageCursor();
        assetPageCursor(nextPage, T13_00, null, D1H, NEXT_PAGE);

        var prevPage = factory.previousPageCursor();
        assetPageCursor(prevPage, T11_30, T13_30, D1H, PREVIOUS_PAGE);
    }

    @Test
    public void sortByDeparture() {
        var factory = new PageCursorFactory(STREET_AND_DEPARTURE_TIME)
                .withOriginalSearch(null, T12_00, T13_30, D1H);

        var nextPage = factory.nextPageCursor();
        assetPageCursor(nextPage, T13_00, null, D1H, NEXT_PAGE);

        var prevPage = factory.previousPageCursor();
        assetPageCursor(prevPage, T11_00, T13_30, D1H, PREVIOUS_PAGE);
    }

    @Test
    public void sortByDepartureCropSearchWindow() {
        var factory = new PageCursorFactory(STREET_AND_DEPARTURE_TIME)
                .withOriginalSearch(PREVIOUS_PAGE, T12_00, T13_30, D1H)
                .withRemovedItineraries(T12_30, T13_00);

        var nextPage = factory.nextPageCursor();
        assetPageCursor(nextPage, T13_00, null, D1H, NEXT_PAGE);

        var prevPage = factory.previousPageCursor();
        assetPageCursor(prevPage, T11_30, T13_00, D1H, PREVIOUS_PAGE);
    }

    @Test
    public void sortByDepartureNextPage() {
        var factory = new PageCursorFactory(STREET_AND_DEPARTURE_TIME)
                .withOriginalSearch(NEXT_PAGE, T12_00, T13_30, D1H);

        var nextPage = factory.nextPageCursor();
        assetPageCursor(nextPage, T13_00, null, D1H, NEXT_PAGE);

        var prevPage = factory.previousPageCursor();
        assetPageCursor(prevPage, T11_00, T12_30, D1H, PREVIOUS_PAGE);
    }

    @Test
    public void sortByDepartureCropSearchWindowNextPage() {
        var factory = new PageCursorFactory(STREET_AND_DEPARTURE_TIME)
                .withOriginalSearch(NEXT_PAGE, T12_00, T13_30, D1H)
                .withRemovedItineraries(T12_30, T13_00);

        var nextPage = factory.nextPageCursor();
        assetPageCursor(nextPage, T12_30, null, D1H, NEXT_PAGE);

        var prevPage = factory.previousPageCursor();
        assetPageCursor(prevPage, T11_00, T12_30, D1H, PREVIOUS_PAGE);
    }

    private void assetPageCursor(
            PageCursor pageCursor,
            Instant expEdt,
            Instant expLat,
            Duration expSearchWindow,
            PageType expPageType
    ) {
        assertEquals(expEdt, pageCursor.earliestDepartureTime);
        assertEquals(expLat, pageCursor.latestArrivalTime);
        assertEquals(expSearchWindow, pageCursor.searchWindow);
        assertEquals(expPageType, pageCursor.type);
    }

    private static Instant time(String input) {
        return TIME_ZERO.plusSeconds(TimeUtils.time(input));
    }
}