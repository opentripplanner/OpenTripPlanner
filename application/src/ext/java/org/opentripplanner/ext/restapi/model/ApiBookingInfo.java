package org.opentripplanner.ext.restapi.model;

import java.io.Serializable;
import java.time.Duration;
import java.util.Set;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Info about how a trip might be booked at a particular stop. All of this is pass-through
 * information, except information about booking time and booking notice.
 */
public class ApiBookingInfo implements Serializable {

  /**
   * How to contact the agency to book a trip or requests information.
   */
  public final ApiContactInfo contactInfo;

  /**
   * What booking methods are available at this stop time.
   */
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

  /**
   * Message to riders utilizing service at a stop_time when booking on-demand pickup and drop off.
   * Meant to provide minimal information to be transmitted within a user interface about the action
   * a rider must take in order to utilize the service.
   */
  public final String message;

  /**
   * Functions in the same way as message but used when riders have on-demand pickup only.
   */
  public final String pickupMessage;

  /**
   * Functions in the same way as message but used when riders have on-demand drop off only.
   */
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
    } else {
      this.minimumBookingNoticeSeconds = null;
    }

    if (maximumBookingNotice != null) {
      this.maximumBookingNoticeSeconds = (int) maximumBookingNotice.toSeconds();
    } else {
      this.maximumBookingNoticeSeconds = null;
    }
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(getClass())
      .addObj("contactInfo", contactInfo)
      .addCol("bookingMethods", bookingMethods)
      .addObj("earliestBookingTime", earliestBookingTime)
      .addObj("latestBookingTime", latestBookingTime)
      .addNum("minimumBookingNoticeSeconds", minimumBookingNoticeSeconds)
      .addNum("maximumBookingNoticeSeconds", maximumBookingNoticeSeconds)
      .addStr("message", message)
      .addStr("pickupMessage", pickupMessage)
      .addStr("dropOffMessage", dropOffMessage)
      .toString();
  }
}
