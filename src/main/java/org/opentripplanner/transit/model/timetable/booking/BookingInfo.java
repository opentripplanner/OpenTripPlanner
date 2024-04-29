package org.opentripplanner.transit.model.timetable.booking;

import java.io.Serializable;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.transit.model.organization.ContactInfo;

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

  /**
   * Create new booking-info for routing based on the parameters used by the trip planner
   * routing. The {@code latestBookingTime} and {@code minimumBookingNotice} is checked
   * to make sure the service is bookable using the request parameter {@code bookingTime}.
   * <p>
   * This method returns empty if this object does not contain parameters used by the
   * routing algorithm.
   *<p>
   * @param flexTripAccessTime The time used for access before boarding the Flex trip - it could
   *                           include slack or walk time.
   */
  @Nullable
  public Optional<RoutingBookingInfo> createRoutingBookingInfo(int flexTripAccessTime) {
    if (minimumBookingNotice == null && latestBookingTime == null) {
      return Optional.empty();
    }
    var builder = RoutingBookingInfo.of();

    if (latestBookingTime != null) {
      // TODO TGR BOOKING_TIME - This do not look right. Why would we remove the time it takes to
      //     walk to a flex area here. This is about the time we want to book. I could see how this
      //     should apply to the `minimumBookingNotice` because it is relative to the board-time
      //     but not the `latestBookingTime`.
      // builder.withLatestBookingTime(latestBookingTime.relativeTime() - flexTripAccessTime);
      builder.withLatestBookingTime(latestBookingTime.relativeTimeSeconds());
    }
    if (minimumBookingNotice != null) {
      // builder.withMinimumBookingNotice((int) minimumBookingNotice.toSeconds());
      builder.withMinimumBookingNotice((int) minimumBookingNotice.toSeconds() - flexTripAccessTime);
    }
    return Optional.of(builder.build());
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(BookingInfo.class)
      .addObj("cntactInfo", contactInfo)
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
