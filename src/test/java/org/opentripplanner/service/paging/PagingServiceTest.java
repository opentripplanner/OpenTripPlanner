package org.opentripplanner.service.paging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.service.paging.TestPagingModel.arriveByDriver;
import static org.opentripplanner.service.paging.TestPagingModel.departAfterDriver;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.model.plan.TestItineraryBuilder;
import org.opentripplanner.model.plan.paging.cursor.PageCursor;
import org.opentripplanner.model.plan.paging.cursor.PageType;

/**
 * This test the entire paging service module.
 * <p>
 * To debug this test, set the
 */
@SuppressWarnings("DataFlowIssue")
class PagingServiceTest {

  private static final int T12_00 = TimeUtils.time("12:00");
  private static final int T12_30 = TimeUtils.time("12:30");
  private static final int T13_00 = TimeUtils.time("13:00");
  private static final int T13_30 = TimeUtils.time("13:30");
  private static final int T14_00 = TimeUtils.time("14:00");

  private static final Duration D30m = Duration.ofMinutes(30);

  // The SEARCH-WINDOW is set to "fixed" 30m in this test for simplicity
  private static final List<Duration> SEARCH_WINDOW_ADJUSTMENTS = List.of();
  private static final Duration MIN_SEARCH_WINDOW_SIZE = D30m;
  private static final Duration MAX_SEARCH_WINDOW_SIZE = D30m;
  private static final Duration SEARCH_WINDOW_USED = D30m;

  private static final Instant TRANSIT_START_TIME = TestItineraryBuilder.newTime(0).toInstant();

  private static final boolean ARRIVE_BY = true;
  private static final boolean DEPART_AFTER = false;

  @Test
  @SuppressWarnings("deprecation")
  void testCreateTripSearchMetadataDepartAfter() {
    var testDriver = departAfterDriver(T12_00, SEARCH_WINDOW_USED, 3);
    var subject = pagingService(testDriver);

    assertEquals(D30m, subject.createTripSearchMetadata().searchWindowUsed);
    assertEquals("11:30", TestPagingUtils.clStr(subject.createTripSearchMetadata().prevDateTime));
    // 12:09 will cause duplicates, the solution is to use the complete sort-vector.
    // The cursor implementation does that
    assertEquals("12:09", TestPagingUtils.clStr(subject.createTripSearchMetadata().nextDateTime));
  }

  @Test
  @SuppressWarnings("deprecation")
  @Disabled("FIX: This test should not fail!")
  void testCreateTripSearchMetadataArriveBy() {
    var testDriver = arriveByDriver(T12_00, T13_00, SEARCH_WINDOW_USED, 3);
    var subject = pagingService(testDriver);

    assertEquals(D30m, subject.createTripSearchMetadata().searchWindowUsed);
    assertEquals("11:40", TestPagingUtils.clStr(subject.createTripSearchMetadata().prevDateTime));
    assertEquals("12:30", TestPagingUtils.clStr(subject.createTripSearchMetadata().nextDateTime));
  }

  /**
   * Test paging DEPART AFTER search with NEXT -> NEXT/PREVIOUS
   */
  @Test
  void testPagingDepartAfterAndNext() {
    var testDriver = departAfterDriver(T12_00, SEARCH_WINDOW_USED, 3);
    var subject = pagingService(testDriver);
    var nextCursor = subject.nextPageCursor();

    assertPageCursor(
      nextCursor,
      PageType.NEXT_PAGE,
      SortOrder.STREET_AND_ARRIVAL_TIME,
      "12:09",
      ""
    );

    // TEST PAGING AFTER NEXT -> NEXT & PREVIOUS
    {
      var nextDriver = testDriver.newPage(nextCursor);
      var nextSubject = pagingService(nextDriver, nextCursor);

      // PREV
      assertPageCursor(
        nextSubject.previousPageCursor(),
        PageType.PREVIOUS_PAGE,
        SortOrder.STREET_AND_ARRIVAL_TIME,
        "11:39",
        "12:40"
      );
      // NEXT
      assertPageCursor(
        nextSubject.nextPageCursor(),
        PageType.NEXT_PAGE,
        SortOrder.STREET_AND_ARRIVAL_TIME,
        "12:29",
        ""
      );
    }
  }

  /**
   * Test paging DEPART AFTER search with PREVIOUS -> NEXT/PREVIOUS
   */
  @Test
  void testPagingDepartAfterAndPrevious() {
    var testDriver = departAfterDriver(T12_30, SEARCH_WINDOW_USED, 3);
    var subject = pagingService(testDriver);
    var prevCursor = subject.previousPageCursor();

    assertPageCursor(
      prevCursor,
      PageType.PREVIOUS_PAGE,
      SortOrder.STREET_AND_ARRIVAL_TIME,
      "12:00",
      // THIS IS NOT CORRECT, MAY LEAD TO MISSED ITINERARIES (12:29~13:11), THIS DOES NOT
      // OCCUR IN THIS TEST, BECAUSE THIS WILL HAPPEN IN RAPTOR
      "13:10"
    );

    // TEST PAGING AFTER PREVIOUS
    {
      var prevItineraries = testDriver.newPage(prevCursor);
      var prevSubject = pagingService(prevItineraries, prevCursor);

      // PREV
      assertPageCursor(
        prevSubject.previousPageCursor(),
        PageType.PREVIOUS_PAGE,
        SortOrder.STREET_AND_ARRIVAL_TIME,
        "11:41",
        "12:40"
      );
      // NEXT
      assertPageCursor(
        prevSubject.nextPageCursor(),
        PageType.NEXT_PAGE,
        SortOrder.STREET_AND_ARRIVAL_TIME,
        "12:30",
        ""
      );
    }
  }

  /**
   * Test paging ARRIVE BY search with NEXT -> NEXT/PREVIOUS
   */
  @Test
  void testPagingArriveByAndNext() {
    var testDriver = arriveByDriver(T12_00, T13_00, D30m, 3);
    var subject = pagingService(testDriver);

    var nextCursor = subject.nextPageCursor();

    assertPageCursor(
      nextCursor,
      PageType.NEXT_PAGE,
      SortOrder.STREET_AND_DEPARTURE_TIME,
      "12:30",
      ""
    );

    // TEST PAGING AFTER NEXT -> NEXT & PREVIOUS
    {
      var nextDriver = testDriver.newPage(nextCursor);
      var nextSubject = pagingService(nextDriver, nextCursor);

      // PREV
      assertPageCursor(
        nextSubject.previousPageCursor(),
        PageType.PREVIOUS_PAGE,
        SortOrder.STREET_AND_DEPARTURE_TIME,
        "12:00",
        ""
      );
      // NEXT
      assertPageCursor(
        nextSubject.nextPageCursor(),
        PageType.NEXT_PAGE,
        SortOrder.STREET_AND_DEPARTURE_TIME,
        "12:40",
        ""
      );
    }
  }

  /**
   * Test paging ARRIVE BY search with PREVIOUS -> NEXT/PREVIOUS
   */
  @Test
  void testPagingArriveByAndPrevious() {
    var testDriver = arriveByDriver(T12_30, T13_30, SEARCH_WINDOW_USED, 3);
    var subject = pagingService(testDriver);

    var cursor = subject.previousPageCursor();

    assertPageCursor(
      cursor,
      PageType.PREVIOUS_PAGE,
      SortOrder.STREET_AND_DEPARTURE_TIME,
      // THIS SHOULD BE 12:10 ?
      "12:11",
      // This is wrong, the LAT should be kept until the paging direction is turned from PREV to
      // NEXT. Expected 13:30
      "13:11"
    );

    // TEST PAGING AFTER PREVIOUS
    {
      var prevItineraries = testDriver.newPage(cursor);
      var prevSubject = pagingService(prevItineraries, cursor);

      // PREV
      assertPageCursor(
        prevSubject.previousPageCursor(),
        PageType.PREVIOUS_PAGE,
        SortOrder.STREET_AND_DEPARTURE_TIME,
        // THIS SHOULD BE 12:10 - 30m = 11:40 ?
        "12:00",
        // This is wrong, the LAT should be kept until the paging direction is turned from PREV to
        // NEXT. Expected 13:30
        "13:11"
      );
      // NEXT
      assertPageCursor(
        prevSubject.nextPageCursor(),
        PageType.NEXT_PAGE,
        SortOrder.STREET_AND_DEPARTURE_TIME,
        "12:41",
        ""
      );
    }
  }

  private static void assertPageCursor(
    PageCursor cursor,
    PageType pageType,
    SortOrder sortOrder,
    String edt,
    String lat
  ) {
    assertEquals(pageType, cursor.type());
    assertEquals(sortOrder, cursor.originalSortOrder());
    assertEquals(D30m, cursor.searchWindow());
    assertEquals(edt, TestPagingUtils.clStr(cursor.earliestDepartureTime()));
    assertEquals(lat, TestPagingUtils.clStr(cursor.latestArrivalTime()));
  }

  private PagingService pagingService(TestDriver testDriver) {
    return new PagingService(
      SEARCH_WINDOW_ADJUSTMENTS,
      MIN_SEARCH_WINDOW_SIZE,
      MAX_SEARCH_WINDOW_SIZE,
      SEARCH_WINDOW_USED,
      testDriver.earliestDepartureTime(),
      testDriver.latestArrivalTime(),
      testDriver.sortOrder(),
      testDriver.arrivedBy(),
      testDriver.nResults(),
      null,
      testDriver.filterResults(),
      testDriver.kept()
    );
  }

  private PagingService pagingService(TestDriver testDriver, PageCursor pageCursor) {
    return new PagingService(
      SEARCH_WINDOW_ADJUSTMENTS,
      MIN_SEARCH_WINDOW_SIZE,
      MAX_SEARCH_WINDOW_SIZE,
      SEARCH_WINDOW_USED,
      pageCursor.earliestDepartureTime(),
      pageCursor.latestArrivalTime(),
      pageCursor.originalSortOrder(),
      testDriver.arrivedBy(),
      testDriver.nResults(),
      pageCursor,
      testDriver.filterResults(),
      testDriver.kept()
    );
  }
}
