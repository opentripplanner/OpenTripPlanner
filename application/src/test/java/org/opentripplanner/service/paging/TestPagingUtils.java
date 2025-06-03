package org.opentripplanner.service.paging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import org.opentripplanner.model.plan.ItinerarySortKey;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.model.plan.paging.cursor.PageCursor;
import org.opentripplanner.model.plan.paging.cursor.PageType;

class TestPagingUtils {

  /**
   * This method removes everything but the time of day from a DateTime string for each found
   * occurrence. It also removes leading zeros and seconds if they are zero.
   *
   * For example:
   *     2020-02-01T09:00:00Z -> 9:00
   *     2020-02-01T12:00:30Z -> 12:00:30
   */
  static String cleanStr(Object value) {
    if (value == null) {
      return "";
    }
    return value
      .toString()
      .replaceAll("2020-02-01T(\\d\\d:\\d\\d:\\d\\d)Z", "$1-1d")
      .replaceAll("2020-02-02T(\\d\\d:\\d\\d:\\d\\d)Z", "$1")
      .replaceAll("2020-02-03T(\\d\\d:\\d\\d:\\d\\d)Z", "$1+1d")
      .replaceAll("0(\\d:\\d\\d:\\d\\d)", "$1")
      .replaceAll("(\\d?\\d:\\d\\d):00", "$1");
  }

  static String toString(ItinerarySortKey it) {
    if (it == null) {
      return "";
    }
    var value = it.keyAsString();
    value = cleanStr(value);
    // indent cost with one digit
    return value.replaceAll("(\\$\\d,|transit])", " $1");
  }

  static void assertPageCursor(
    PageCursor cursor,
    PageType pageType,
    SortOrder sortOrder,
    String edt,
    String lat,
    Duration searchWindow
  ) {
    assertEquals(pageType, cursor.type());
    assertEquals(sortOrder, cursor.originalSortOrder());
    assertEquals(searchWindow, cursor.searchWindow());
    assertEquals(edt, cleanStr(cursor.earliestDepartureTime()));
    assertEquals(lat, cleanStr(cursor.latestArrivalTime()));
  }
}
