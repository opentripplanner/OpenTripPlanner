package org.opentripplanner.service.paging;

import static org.opentripplanner.service.paging.TestPagingModel.D30m;
import static org.opentripplanner.service.paging.TestPagingModel.T16_15_00;
import static org.opentripplanner.service.paging.TestPagingModel.T16_30_00;
import static org.opentripplanner.service.paging.TestPagingModel.T17_30_00;
import static org.opentripplanner.service.paging.TestPagingUtils.assertPageCursor;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.model.plan.paging.cursor.PageType;

class PS4_NextPageAfterArriveBySearchTest {

  private static final Duration SEARCH_WINDOW_USED = D30m;
  private final TestPagingModel model = TestPagingModel.testDataWithFewItinerariesCaseC();

  @Test
  void testPagingArriveByAndNextMinimum() {
    var testDriver = model.arriveByDriver(T16_15_00, T17_30_00, SEARCH_WINDOW_USED, 1);
    var subject = testDriver.pagingService();

    var nextCursor = subject.nextPageCursor();

    // The earliest departure time is 16:45:00 instead of 16:45:30, because determining it from
    // the latest itinerary departure would make it go over the search window,
    // minimum of 16:45:00 and 16:45:30.
    assertPageCursor(
      nextCursor,
      PageType.NEXT_PAGE,
      SortOrder.STREET_AND_DEPARTURE_TIME,
      "16:45",
      "",
      SEARCH_WINDOW_USED
    );
  }

  @Test
  void testPagingArriveByAndNextSeconds() {
    var testDriver = model.arriveByDriver(T16_30_00, T17_30_00, SEARCH_WINDOW_USED, 1);
    var subject = testDriver.pagingService();

    var nextCursor = subject.nextPageCursor();

    // The earliest departure time is 16:45:30 here, because it is determined from the
    // latest itinerary departure from the results.
    assertPageCursor(
      nextCursor,
      PageType.NEXT_PAGE,
      SortOrder.STREET_AND_DEPARTURE_TIME,
      "16:45:30",
      "",
      SEARCH_WINDOW_USED
    );
  }
}
