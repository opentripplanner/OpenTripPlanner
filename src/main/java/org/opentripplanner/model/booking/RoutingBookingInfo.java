package org.opentripplanner.model.booking;

import java.time.Duration;
import org.opentripplanner.model.BookingTime;

/**
 * This is the contract between booking info and the router. The router will enforce
 * this information if the request sets the earliest-booking-time request parameter.
 */
public interface RoutingBookingInfo {
  /**
   * The router should enforce that the <em>request earliest-booking-time</em> is before
   * this time. This time is relative to the service/operating date of the actual trip.
   */
  BookingTime getLatestBookingTime();

  /**
   * The router is responsible for enforcing that it is enough time between the
   * <em>request earliest-booking-time</em> plus this {@code minimumBookingNotice} and the
   * passenger trip boarding time calculated by OTP.
   */
  Duration getMinimumBookingNotice();
}
