package org.opentripplanner.api.model;

import java.io.Serializable;
import java.time.Duration;
import java.util.Set;

/**
 * Info about how a trip might be booked at a particular stop. All of this is pass-through
 * information, except information about booking time and booking notice.
 *
 */
public class ApiBookingInfo implements Serializable {

  public final ApiContactInfo contactInfo;

  public final Set<String> bookingMethods;

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
  public final Integer minimumBookingNoticeSeconds;

  /**
   * Cannot be set at the same time as earliestBookingTime or latestBookingTime
   */
  public final Integer maximumBookingNoticeSeconds;

  public final String message;

  public final String pickupMessage;

  public final String dropOffMessage;

  public ApiBookingInfo(
      ApiContactInfo contactInfo,
      Set<String> bookingMethods,
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

    this.earliestBookingTime = earliestBookingTime;
    this.latestBookingTime = latestBookingTime;
    if (minimumBookingNotice != null) {
      this.minimumBookingNoticeSeconds = (int) minimumBookingNotice.toSeconds();
    }
    else {
      this.minimumBookingNoticeSeconds = null;
    }

    if (maximumBookingNotice != null) {
      this.maximumBookingNoticeSeconds = (int) maximumBookingNotice.toSeconds();
    }
    else {
      this.maximumBookingNoticeSeconds = null;
    }
  }

}
