package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.BookingMethod;
import org.rutebanken.netex.model.BookingMethodEnumeration;

/**
 * Maps from NeTEx BookingMethodEnumeration into OTP BookingMethod.
 */
public class BookingMethodMapper {

  public static BookingMethod map(BookingMethodEnumeration bookingMethodEnumeration) {
    switch (bookingMethodEnumeration) {
      case CALL_DRIVER:
        return BookingMethod.CALL_DRIVER;
      case CALL_OFFICE:
        return BookingMethod.CALL_OFFICE;
      case ONLINE:
        return BookingMethod.ONLINE;
      case PHONE_AT_STOP:
        return BookingMethod.PHONE_AT_STOP;
      case TEXT:
        return BookingMethod.TEXT_MESSAGE;
      default:
        return null;
    }
  }
}
