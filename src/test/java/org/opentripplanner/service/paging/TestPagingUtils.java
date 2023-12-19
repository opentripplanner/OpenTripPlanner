package org.opentripplanner.service.paging;

import org.opentripplanner.model.plan.ItinerarySortKey;

class TestPagingUtils {

  static String cleanStr(Object value) {
    if (value == null) {
      return "";
    }
    return value
      .toString()
      .replaceAll("2020-02-01T(\\d\\d:\\d\\d):00Z", "$1-1d")
      .replaceAll("2020-02-02T(\\d\\d:\\d\\d):00Z", "$1")
      .replaceAll("2020-02-03T(\\d\\d:\\d\\d):00Z", "$1+1d")
      .replaceAll("0(\\d:00)", "$1");
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
