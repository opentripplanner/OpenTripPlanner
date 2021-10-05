package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.BookingMethod;
import org.rutebanken.netex.model.BookingMethodEnumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps from NeTEx BookingMethodEnumeration into OTP BookingMethod.
 */
public class BookingMethodMapper {

  private static final Logger LOG = LoggerFactory.getLogger(BookingMethodMapper.class);

  public static BookingMethod map(BookingMethodEnumeration bookingMethodEnumeration) {
    if(bookingMethodEnumeration == null) {
      return null;
    }

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
        LOG.warn(
                "Booking method unknown/not supported will be ignored: " + bookingMethodEnumeration
        );
        return null;
    }
  }
}
