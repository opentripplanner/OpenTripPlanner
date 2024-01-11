package org.opentripplanner.ext.restapi.mapping;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.model.BookingMethod;

public class BookingMethodMapper {

  static Set<String> mapBookingMethods(EnumSet<BookingMethod> m) {
    if (m == null) {
      return null;
    }
    return m.stream().map(BookingMethodMapper::mapBookingMethods).collect(Collectors.toSet());
  }

  private static String mapBookingMethods(BookingMethod m) {
    switch (m) {
      case CALL_DRIVER:
        return "CALL_DRIVER";
      case CALL_OFFICE:
        return "CALL_OFFICE";
      case ONLINE:
        return "ONLINE";
      case PHONE_AT_STOP:
        return "PHONE_AT_STOP";
      case TEXT_MESSAGE:
        return "TEXT_MESSAGE";
      default:
        return null;
    }
  }
}
