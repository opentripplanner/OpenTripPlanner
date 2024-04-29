package org.opentripplanner.model;

import java.io.Serializable;
import java.time.Duration;
import java.util.EnumSet;
import javax.annotation.Nullable;
import org.opentripplanner.model.booking.RoutingBookingInfo;
import org.opentripplanner.transit.model.organization.ContactInfo;

/**
 * Info about how a trip might be booked at a particular stop. All of this is pass-through
 * information, except information about booking time and booking notice.
 */
public class BookingInfo implements Serializable, RoutingBookingInfo {

  private final ContactInfo contactInfo;

  private final EnumSet<BookingMethod> bookingMethods;

  /**
   * Cannot be set at the same time as minimumBookingNotice or maximumBookingNotice
   */
  @Nullable
  private final BookingTime earliestBookingTime;

  /**
   * Cannot be set at the same time as minimumBookingNotice or maximumBookingNotice
   */
  @Nullable
  private final BookingTime latestBookingTime;

  /**
   * Cannot be set at the same time as earliestBookingTime or latestBookingTime
   */
  @Nullable
  private final Duration minimumBookingNotice;

  /**
   * Cannot be set at the same time as earliestBookingTime or latestBookingTime
   */
  @Nullable
  private final Duration maximumBookingNotice;

  @Nullable
  private final String message;

  @Nullable
  private final String pickupMessage;

  @Nullable
  private final String dropOffMessage;

  BookingInfo(
    ContactInfo contactInfo,
    EnumSet<BookingMethod> bookingMethods,
    BookingTime earliestBookingTime,
    @Nullable BookingTime latestBookingTime,
    @Nullable Duration minimumBookingNotice,
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

  public ContactInfo getContactInfo() {
    return contactInfo;
  }

  public EnumSet<BookingMethod> bookingMethods() {
    return bookingMethods;
  }

  @Nullable
  public BookingTime getEarliestBookingTime() {
    return earliestBookingTime;
  }

  @Nullable
  public BookingTime getLatestBookingTime() {
    return latestBookingTime;
  }

  @Nullable
  public Duration getMinimumBookingNotice() {
    return minimumBookingNotice;
  }

  @Nullable
  public Duration getMaximumBookingNotice() {
    return maximumBookingNotice;
  }

  @Nullable
  public String getMessage() {
    return message;
  }

  @Nullable
  public String getPickupMessage() {
    return pickupMessage;
  }

  @Nullable
  public String getDropOffMessage() {
    return dropOffMessage;
  }
}
