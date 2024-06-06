package org.opentripplanner.transit.model.timetable.booking;

import java.time.Duration;
import java.util.EnumSet;
import org.opentripplanner.transit.model.organization.ContactInfo;

public class BookingInfoBuilder {

  ContactInfo contactInfo;
  EnumSet<BookingMethod> bookingMethods;
  BookingTime earliestBookingTime;
  BookingTime latestBookingTime;
  Duration minimumBookingNotice;
  Duration maximumBookingNotice;
  String message;
  String pickupMessage;
  String dropOffMessage;

  BookingInfoBuilder() {}

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
    return new BookingInfo(this);
  }
}
