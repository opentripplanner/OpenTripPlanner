package org.opentripplanner.model;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.function.IntUnaryOperator;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.transit.model.organization.ContactInfo;

/**
 * Info about how a trip might be booked at a particular stop. All of this is pass-through
 * information, except information about booking time and booking notice.
 * <p>
 * // TODO Make the routing take into account booking time and booking notice.
 */
public class BookingInfo implements Serializable {

  private static final int DAY_IN_SECONDS = 3600 * 24;

  private final ContactInfo contactInfo;

  private final EnumSet<BookingMethod> bookingMethods;

  /**
   * Cannot be set at the same time as minimumBookingNotice or maximumBookingNotice
   */
  private final BookingTime earliestBookingTime;

  /**
   * Cannot be set at the same time as minimumBookingNotice or maximumBookingNotice
   */
  private final BookingTime latestBookingTime;

  /**
   * Cannot be set at the same time as earliestBookingTime or latestBookingTime
   */
  private final Duration minimumBookingNotice;

  /**
   * Cannot be set at the same time as earliestBookingTime or latestBookingTime
   */
  private final Duration maximumBookingNotice;

  private final String message;

  private final String pickupMessage;

  private final String dropOffMessage;

  public BookingInfo(
    ContactInfo contactInfo,
    EnumSet<BookingMethod> bookingMethods,
    BookingTime earliestBookingTime,
    BookingTime latestBookingTime,
    Duration minimumBookingNotice,
    Duration maximumBookingNotice,
    String message,
    String pickupMessage,
    String dropOffMessage
  ) {
    this.contactInfo = contactInfo;
    this.bookingMethods = bookingMethods;
    this.message = message;
    this.pickupMessage = pickupMessage;
    this.dropOffMessage = dropOffMessage;

    // Ensure that earliestBookingTime/latestBookingTime is not set at the same time as
    // minimumBookingNotice/maximumBookingNotice
    if (earliestBookingTime != null || latestBookingTime != null) {
      this.earliestBookingTime = earliestBookingTime;
      this.latestBookingTime = latestBookingTime;
      this.minimumBookingNotice = null;
      this.maximumBookingNotice = null;
    } else if (minimumBookingNotice != null || maximumBookingNotice != null) {
      this.earliestBookingTime = null;
      this.latestBookingTime = null;
      this.minimumBookingNotice = minimumBookingNotice;
      this.maximumBookingNotice = maximumBookingNotice;
    } else {
      this.earliestBookingTime = null;
      this.latestBookingTime = null;
      this.minimumBookingNotice = null;
      this.maximumBookingNotice = null;
    }
  }

  public int  earliestDepartureTime(int requestedDepartureTime, int earliestBookingTime, IntUnaryOperator getEarliestDepartureTime) {
    int edt = getEarliestDepartureTime.applyAsInt(requestedDepartureTime);
    if(edt == RaptorConstants.TIME_NOT_SET) {
      return RaptorConstants.TIME_NOT_SET;
    }
    if (latestBookingTime != null) {
      int otpLatestBookingTime = calculateOtpTime(
        latestBookingTime.getTime(),
        -latestBookingTime.getDaysPrior()
      );
      if (earliestBookingTime <= otpLatestBookingTime) {
        return edt;
      } else {
        return RaptorConstants.TIME_NOT_SET;
      }
    }
    if (minimumBookingNotice != null) {
      if (edt >= earliestBookingTime + minimumBookingNotice.toSeconds()) {
        return edt;
      } else {
        return getEarliestDepartureTime.applyAsInt(earliestBookingTime + (int) minimumBookingNotice.toSeconds());
      }
    }
    // missing booking info
    return edt;
  }

  static int calculateOtpTime(LocalTime time, int dayOffset) {
    return time.toSecondOfDay() + DAY_IN_SECONDS * dayOffset;
  }

  public ContactInfo getContactInfo() {
    return contactInfo;
  }

  public EnumSet<BookingMethod> bookingMethods() {
    return bookingMethods;
  }

  public BookingTime getEarliestBookingTime() {
    return earliestBookingTime;
  }

  public BookingTime getLatestBookingTime() {
    return latestBookingTime;
  }

  public Duration getMinimumBookingNotice() {
    return minimumBookingNotice;
  }

  public Duration getMaximumBookingNotice() {
    return maximumBookingNotice;
  }

  public String getMessage() {
    return message;
  }

  public String getPickupMessage() {
    return pickupMessage;
  }

  public String getDropOffMessage() {
    return dropOffMessage;
  }
}
