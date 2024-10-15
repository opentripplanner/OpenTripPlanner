package org.opentripplanner.service.paging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.service.paging.TestPagingModel.D30m;
import static org.opentripplanner.service.paging.TestPagingModel.T12_00;
import static org.opentripplanner.service.paging.TestPagingModel.T12_30;
import static org.opentripplanner.service.paging.TestPagingModel.T13_00;
import static org.opentripplanner.service.paging.TestPagingModel.T13_30;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.model.plan.paging.cursor.PageCursor;
import org.opentripplanner.model.plan.paging.cursor.PageType;

/**
 * This test focus on testing the paging with many itineraries which all is pareto-optimal.
 * <p>
 * Each test do a next/prev search followed by another search (both prev/next) and verify
 * all properties in the page-token. There are 4 test:
 * <pre>
 * - DEPART AFTER search -> verify NEXT -> search next -> verify NEXT & PREVIOUS
 * - DEPART AFTER search -> verify PREVIOUS -> search next -> verify NEXT & PREVIOUS
 * - ARRIVE BY search -> verify NEXT -> search previous -> verify NEXT & PREVIOUS
 * - ARRIVE BY search -> verify PREVIOUS -> search previous -> verify NEXT & PREVIOUS
 * </pre>
 * Note! We are not doing the actual search, just emulating the search using the
 * {@link TestDriver} mock.
 * <p>
 * All components required to test paging is used including the {@link PagingService} and the
 * 3 filters:
 * <ol>
 *   <li>PagingFilter</li>
 *   <li>OutsideSearchWindowFilter</li>
 *   <li>NumItinerariesFilter</li>
 * </ol>
 * <p>
 */
@SuppressWarnings("DataFlowIssue")
class PS2_ManyParetoOptimalItinerariesTest {

  private static final Duration SEARCH_WINDOW_USED = D30m;
  private final TestPagingModel model = TestPagingModel.testDataWithManyItinerariesCaseA();

  /**
   * Test paging DEPART AFTER search with NEXT -> NEXT/PREVIOUS
   */
  @Test
  void testPagingDepartAfterAndNext() {
    var testDriver = model.departAfterDriver(T12_00, SEARCH_WINDOW_USED, 3);
    var subject = testDriver.pagingService();
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
      var nextSubject = nextDriver.pagingService(nextCursor);

      // PREV
      assertPageCursor(
        nextSubject.previousPageCursor(),
        PageType.PREVIOUS_PAGE,
        SortOrder.STREET_AND_ARRIVAL_TIME,
        "11:39",
        ""
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
    var testDriver = model.departAfterDriver(T12_30, SEARCH_WINDOW_USED, 3);
    var subject = testDriver.pagingService();
    var prevCursor = subject.previousPageCursor();

    assertPageCursor(
      prevCursor,
      PageType.PREVIOUS_PAGE,
      SortOrder.STREET_AND_ARRIVAL_TIME,
      "12:00",
      ""
    );

    // TEST PAGING AFTER PREVIOUS
    {
      var prevDriver = testDriver.newPage(prevCursor);
      var prevSubject = prevDriver.pagingService(prevCursor);

      // PREV
      assertPageCursor(
        prevSubject.previousPageCursor(),
        PageType.PREVIOUS_PAGE,
        SortOrder.STREET_AND_ARRIVAL_TIME,
        "11:41",
        ""
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
    var testDriver = model.arriveByDriver(T12_00, T13_00, D30m, 3);
    var subject = testDriver.pagingService();

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
      var nextSubject = nextDriver.pagingService(nextCursor);

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
    var testDriver = model.arriveByDriver(T12_30, T13_30, SEARCH_WINDOW_USED, 3);
    var subject = testDriver.pagingService();

    var cursor = subject.previousPageCursor();

    assertPageCursor(
      cursor,
      PageType.PREVIOUS_PAGE,
      SortOrder.STREET_AND_DEPARTURE_TIME,
      "12:11",
      "13:30"
    );

    // TEST PAGING AFTER PREVIOUS
    {
      var prevDriver = testDriver.newPage(cursor);
      var prevSubject = prevDriver.pagingService(cursor);

      // PREV
      assertPageCursor(
        prevSubject.previousPageCursor(),
        PageType.PREVIOUS_PAGE,
        SortOrder.STREET_AND_DEPARTURE_TIME,
        "12:00",
        "13:30"
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
    assertEquals(edt, TestPagingUtils.cleanStr(cursor.earliestDepartureTime()));
    assertEquals(lat, TestPagingUtils.cleanStr(cursor.latestArrivalTime()));
  }
}
