package org.opentripplanner.model.plan.paging.cursor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.SortOrder.STREET_AND_ARRIVAL_TIME;
import static org.opentripplanner.model.plan.SortOrder.STREET_AND_DEPARTURE_TIME;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.model.plan.paging.cursor.PageType.NEXT_PAGE;
import static org.opentripplanner.model.plan.paging.cursor.PageType.PREVIOUS_PAGE;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ItinerarySortKey;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.utils.time.TimeUtils;

@SuppressWarnings("ConstantConditions")
class PageCursorFactoryTest implements PlanTestConstants {

  static final Instant TIME_ZERO = Instant.parse("2020-02-02T00:00:00Z");

  public static final Duration D1H = Duration.ofHours(1);
  public static final Duration D90M = Duration.ofMinutes(90);

  private static final Instant T10_30 = time("10:30");
  private static final Instant T11_01 = time("11:01");
  private static final Instant T12_00 = time("12:00");
  private static final Instant T12_10 = time("12:10");
  private static final Instant T12_30 = time("12:30");
  private static final Instant T13_00 = time("13:00");
  private static final Instant T13_30 = time("13:30");

  @Test
  public void sortArrivalAscending() {
    var factory = new PageCursorFactory(STREET_AND_ARRIVAL_TIME, D90M).withOriginalSearch(
      null,
      T12_00,
      null,
      D1H
    );

    var nextPage = factory.nextPageCursor();
    assertPageCursor(nextPage, T13_00, null, D90M, NEXT_PAGE, false);

    var prevPage = factory.previousPageCursor();
    assertPageCursor(prevPage, T10_30, null, D90M, PREVIOUS_PAGE, false);
  }

  @Test
  public void sortArrivalAscendingCropSearchWindow() {
    var factory = new PageCursorFactory(STREET_AND_ARRIVAL_TIME, D90M)
      .withOriginalSearch(NEXT_PAGE, T12_00, null, D1H)
      .withRemovedItineraries(
        new TestPageCursorInput(
          newItinerary(A).bus(55, timeAsSeconds(T12_00), timeAsSeconds(T12_10), B).build(),
          newItinerary(A).bus(65, timeAsSeconds(T12_30), timeAsSeconds(T13_30), B).build()
        )
      );

    var nextPage = factory.nextPageCursor();
    assertPageCursor(nextPage, T12_30, null, D90M, NEXT_PAGE, true);

    var prevPage = factory.previousPageCursor();
    assertPageCursor(prevPage, T10_30, null, D90M, PREVIOUS_PAGE, true);
  }

  @Test
  public void sortArrivalAscendingPreviousPage() {
    var factory = new PageCursorFactory(STREET_AND_ARRIVAL_TIME, D90M).withOriginalSearch(
      PREVIOUS_PAGE,
      T12_00,
      null,
      D1H
    );

    var nextPage = factory.nextPageCursor();
    assertPageCursor(nextPage, T13_00, null, D90M, NEXT_PAGE, false);

    var prevPage = factory.previousPageCursor();
    assertPageCursor(prevPage, T10_30, null, D90M, PREVIOUS_PAGE, false);
  }

  @Test
  public void sortArrivalAscendingCropSearchWindowPreviousPage() {
    var factory = new PageCursorFactory(STREET_AND_ARRIVAL_TIME, D90M)
      .withOriginalSearch(PREVIOUS_PAGE, T12_00, null, D1H)
      .withRemovedItineraries(
        new TestPageCursorInput(
          newItinerary(A).bus(55, timeAsSeconds(T12_00), timeAsSeconds(T12_10), B).build(),
          newItinerary(A).bus(65, timeAsSeconds(T12_30), timeAsSeconds(T13_30), B).build()
        )
      );

    var nextPage = factory.nextPageCursor();
    assertPageCursor(nextPage, T13_00, null, D90M, NEXT_PAGE, true);

    var prevPage = factory.previousPageCursor();
    assertPageCursor(prevPage, T11_01, null, D90M, PREVIOUS_PAGE, true);
  }

  @Test
  public void sortDepartureDescending() {
    var factory = new PageCursorFactory(STREET_AND_DEPARTURE_TIME, D90M).withOriginalSearch(
      null,
      T12_00,
      T13_30,
      D1H
    );

    var nextPage = factory.nextPageCursor();
    assertPageCursor(nextPage, T13_00, null, D90M, NEXT_PAGE, false);

    var prevPage = factory.previousPageCursor();
    assertPageCursor(prevPage, T10_30, T13_30, D90M, PREVIOUS_PAGE, false);
  }

  @Test
  public void sortDepartureDescendingCropSearchWindow() {
    var factory = new PageCursorFactory(STREET_AND_DEPARTURE_TIME, D90M)
      .withOriginalSearch(PREVIOUS_PAGE, T12_00, T13_30, D1H)
      .withRemovedItineraries(
        new TestPageCursorInput(
          newItinerary(A).bus(55, timeAsSeconds(T12_00), timeAsSeconds(T12_30), B).build(),
          newItinerary(A).bus(65, timeAsSeconds(T12_30), timeAsSeconds(T13_00), B).build()
        )
      );

    var nextPage = factory.nextPageCursor();
    assertPageCursor(nextPage, T13_00, null, D90M, NEXT_PAGE, true);

    var prevPage = factory.previousPageCursor();
    assertPageCursor(prevPage, T11_01, T13_30, D90M, PREVIOUS_PAGE, true);
  }

  @Test
  public void sortDepartureDescendingNextPage() {
    var factory = new PageCursorFactory(STREET_AND_DEPARTURE_TIME, D90M).withOriginalSearch(
      NEXT_PAGE,
      T12_00,
      T13_30,
      D1H
    );

    var nextPage = factory.nextPageCursor();
    assertPageCursor(nextPage, T13_00, null, D90M, NEXT_PAGE, false);

    var prevPage = factory.previousPageCursor();
    assertPageCursor(prevPage, T10_30, T13_30, D90M, PREVIOUS_PAGE, false);
  }

  @Test
  public void sortDepartureDescendingCropSearchWindowNextPage() {
    var factory = new PageCursorFactory(STREET_AND_DEPARTURE_TIME, D90M)
      .withOriginalSearch(NEXT_PAGE, T12_00, T13_30, D1H)
      .withRemovedItineraries(
        new TestPageCursorInput(
          newItinerary(A).bus(55, timeAsSeconds(T12_00), timeAsSeconds(T12_30), B).build(),
          newItinerary(A).bus(65, timeAsSeconds(T12_30), timeAsSeconds(T13_00), B).build()
        )
      );

    var nextPage = factory.nextPageCursor();
    assertPageCursor(nextPage, T12_30, null, D90M, NEXT_PAGE, true);

    var prevPage = factory.previousPageCursor();
    assertPageCursor(prevPage, T10_30, T13_30, D90M, PREVIOUS_PAGE, true);
  }

  private static Instant time(String input) {
    return TIME_ZERO.plusSeconds(TimeUtils.time(input));
  }

  private static int timeAsSeconds(Instant in) {
    return (int) Duration.between(TIME_ZERO, in).getSeconds();
  }

  private void assertPageCursor(
    PageCursor pageCursor,
    Instant expEdt,
    Instant expLat,
    Duration expSearchWindow,
    PageType expPageType,
    Boolean hasDedupeParams
  ) {
    assertEquals(expEdt, pageCursor.earliestDepartureTime());
    assertEquals(expLat, pageCursor.latestArrivalTime());
    assertEquals(expSearchWindow, pageCursor.searchWindow());
    assertEquals(expPageType, pageCursor.type());
    assertEquals(hasDedupeParams, pageCursor.itineraryPageCut() != null);
  }

  private record TestPageCursorInput(
    Instant earliestKeptArrival,
    Instant earliestRemovedDeparture,
    Instant latestRemovedDeparture,
    Instant latestRemovedArrival,
    ItinerarySortKey pageCut
  )
    implements PageCursorInput {
    public TestPageCursorInput(Itinerary keptItinerary, Itinerary removedItinerary) {
      this(
        keptItinerary.endTimeAsInstant(),
        removedItinerary.startTimeAsInstant(),
        removedItinerary.startTimeAsInstant(),
        removedItinerary.endTimeAsInstant(),
        removedItinerary
      );
    }
  }
}
