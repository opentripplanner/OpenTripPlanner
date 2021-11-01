package org.opentripplanner.api.model;

import java.io.Serializable;
import java.time.Duration;
import java.util.EnumSet;

/**
 * Info about how a trip might be booked at a particular stop. All of this is pass-through
 * information, except information about booking time and booking notice.
 *
 */
public class ApiBookingInfo implements Serializable {

  public final ApiContactInfo contactInfo;

  public final EnumSet<ApiBookingMethod> bookingMethods;

  /**
   * Cannot be set at the same time as minimumBookingNotice or maximumBookingNotice
   */
  public final ApiBookingTime earliestBookingTime;

  /**
   * Cannot be set at the same time as minimumBookingNotice or maximumBookingNotice
   */
  public final ApiBookingTime latestBookingTime;

  /**
   * Cannot be set at the same time as earliestBookingTime or latestBookingTime
   */
  public final Duration minimumBookingNotice;

  /**
   * Cannot be set at the same time as earliestBookingTime or latestBookingTime
   */
  public final Duration maximumBookingNotice;

  public final String message;

  public final String pickupMessage;

  public final String dropOffMessage;

  public ApiBookingInfo(
      ApiContactInfo contactInfo,
      EnumSet<ApiBookingMethod> bookingMethods,
      ApiBookingTime earliestBookingTime,
      ApiBookingTime latestBookingTime,
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

}
