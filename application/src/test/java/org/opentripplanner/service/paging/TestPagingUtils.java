package org.opentripplanner.service.paging;

import org.opentripplanner.model.plan.ItinerarySortKey;

class TestPagingUtils {

  /**
   * This method changes the format of a DateTime string for tests.
   * It also removes leading zeros from an hh:mm string.
   */
  static String cleanStr(Object value) {
    if (value == null) {
      return "";
    }
    return value
      .toString()
      .replaceAll("2020-02-01T(\\d\\d:\\d\\d):00Z", "$1-1d")
      .replaceAll("2020-02-02T(\\d\\d:\\d\\d):00Z", "$1")
      .replaceAll("2020-02-03T(\\d\\d:\\d\\d):00Z", "$1+1d")
      .replaceAll("0(\\d:\\d\\d)", "$1");
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
}
