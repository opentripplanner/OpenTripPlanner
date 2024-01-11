package org.opentripplanner.model;

import java.time.Duration;
import java.util.EnumSet;
import org.opentripplanner.transit.model.organization.ContactInfo;

public class BookingInfoBuilder {

  private ContactInfo contactInfo;
  private EnumSet<BookingMethod> bookingMethods;
  private BookingTime earliestBookingTime;
  private BookingTime latestBookingTime;
  private Duration minimumBookingNotice;
  private Duration maximumBookingNotice;
  private String message;
  private String pickupMessage;
  private String dropOffMessage;

  public BookingInfoBuilder withContactInfo(ContactInfo contactInfo) {
    this.contactInfo = contactInfo;
    return this;
  }

  public BookingInfoBuilder withBookingMethods(EnumSet<BookingMethod> bookingMethods) {
    this.bookingMethods = bookingMethods;
    return this;
  }

  public BookingInfoBuilder withEarliestBookingTime(BookingTime earliestBookingTime) {
    this.earliestBookingTime = earliestBookingTime;
    return this;
  }

  public BookingInfoBuilder withLatestBookingTime(BookingTime latestBookingTime) {
    this.latestBookingTime = latestBookingTime;
    return this;
  }

  public BookingInfoBuilder withMinimumBookingNotice(Duration minimumBookingNotice) {
    this.minimumBookingNotice = minimumBookingNotice;
    return this;
  }

  public BookingInfoBuilder withMaximumBookingNotice(Duration maximumBookingNotice) {
    this.maximumBookingNotice = maximumBookingNotice;
    return this;
  }

  public BookingInfoBuilder withMessage(String message) {
    this.message = message;
    return this;
  }

  public BookingInfoBuilder withPickupMessage(String pickupMessage) {
    this.pickupMessage = pickupMessage;
    return this;
  }

  public BookingInfoBuilder withDropOffMessage(String dropOffMessage) {
    this.dropOffMessage = dropOffMessage;
    return this;
  }

  public BookingInfo build() {
    return new BookingInfo(
      contactInfo,
      bookingMethods,
      earliestBookingTime,
      latestBookingTime,
      minimumBookingNotice,
      maximumBookingNotice,
      message,
      pickupMessage,
      dropOffMessage
    );
  }
}
