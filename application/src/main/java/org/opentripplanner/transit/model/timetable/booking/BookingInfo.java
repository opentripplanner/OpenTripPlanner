package org.opentripplanner.transit.model.timetable.booking;

import java.io.Serializable;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.organization.ContactInfo;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Info about how a trip might be booked at a particular stop. All of this is pass-through
 * information, except information about booking time and booking notice.
 */
public class BookingInfo implements Serializable {

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

  BookingInfo(BookingInfoBuilder builder) {
    this.contactInfo = builder.contactInfo;
    this.bookingMethods = builder.bookingMethods;
    this.message = builder.message;
    this.pickupMessage = builder.pickupMessage;
    this.dropOffMessage = builder.dropOffMessage;

    // Ensure that earliestBookingTime/latestBookingTime is not set at the same time as
    // minimumBookingNotice/maximumBookingNotice
    if (builder.earliestBookingTime != null || builder.latestBookingTime != null) {
      this.earliestBookingTime = builder.earliestBookingTime;
      this.latestBookingTime = builder.latestBookingTime;
      this.minimumBookingNotice = null;
      this.maximumBookingNotice = null;
    } else if (builder.minimumBookingNotice != null || builder.maximumBookingNotice != null) {
      this.earliestBookingTime = null;
      this.latestBookingTime = null;
      this.minimumBookingNotice = builder.minimumBookingNotice;
      this.maximumBookingNotice = builder.maximumBookingNotice;
    } else {
      this.earliestBookingTime = null;
      this.latestBookingTime = null;
      this.minimumBookingNotice = null;
      this.maximumBookingNotice = null;
    }
  }

  public static BookingInfoBuilder of() {
    return new BookingInfoBuilder();
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

  public Optional<Duration> getMinimumBookingNotice() {
    return Optional.ofNullable(minimumBookingNotice);
  }

  public Optional<Duration> getMaximumBookingNotice() {
    return Optional.ofNullable(maximumBookingNotice);
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

  @Override
  public String toString() {
    return ToStringBuilder.of(BookingInfo.class)
      .addObj("contactInfo", contactInfo)
      .addObj("bookingMethods", bookingMethods)
      .addObj("earliestBookingTime", earliestBookingTime)
      .addObj("latestBookingTime", latestBookingTime)
      .addDuration("minimumBookingNotice", minimumBookingNotice)
      .addDuration("maximumBookingNotice", maximumBookingNotice)
      .addStr("message", message)
      .addStr("pickupMessage", pickupMessage)
      .addStr("dropOffMessage", dropOffMessage)
      .toString();
  }
}
