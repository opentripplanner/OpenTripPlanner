package org.opentripplanner.model;

import org.opentripplanner.netex.mapping.BookingTime;

import java.io.Serializable;
import java.time.Duration;
import java.util.EnumSet;

/**
 * Info about how a trip might be booked at a particular stop. All of this is pass-through
 * information, except information about booking time and booking notice.
 *
 * // TODO Make the routing take into account booking time and booking notice.
 */
public class BookingInfo implements Serializable {

  private final ContactInfo contactInfo;

  private final EnumSet<BookingMethod> bookingMethods;

  private final BookingTime earliestBookingTime;

  private final BookingTime latestBookingTime;

  private final Duration minimumBookingNotice;

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
    this.earliestBookingTime = earliestBookingTime;
    this.latestBookingTime = latestBookingTime;
    this.minimumBookingNotice = minimumBookingNotice;
    this.maximumBookingNotice = maximumBookingNotice;
    this.message = message;
    this.pickupMessage = pickupMessage;
    this.dropOffMessage = dropOffMessage;
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
