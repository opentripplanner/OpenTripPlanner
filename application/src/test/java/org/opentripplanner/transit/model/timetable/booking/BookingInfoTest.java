package org.opentripplanner.transit.model.timetable.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.time.LocalTime;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.organization.ContactInfo;

class BookingInfoTest {

  public static final String URL = "http://booking.otp.org";
  public static final ContactInfo CONTACT = ContactInfo.of()
    .withBookingUrl(URL)
    .withContactPerson("Jo Contact")
    .build();
  public static final EnumSet<BookingMethod> BOOKING_METHODS = EnumSet.of(
    BookingMethod.CALL_DRIVER
  );
  public static final BookingTime BOOKING_TIME_NOON = new BookingTime(LocalTime.NOON, 0);

  @Test
  void testBookingInfoWithLatestBookingTime() {
    var subject = BookingInfo.of()
      .withContactInfo(CONTACT)
      .withBookingMethods(BOOKING_METHODS)
      .withLatestBookingTime(BOOKING_TIME_NOON)
      .withMessage("message")
      .withPickupMessage("pickup")
      .withDropOffMessage("dropoff")
      .build();

    assertEquals(CONTACT, subject.getContactInfo());
    assertEquals(BOOKING_METHODS, subject.bookingMethods());
    assertNull(subject.getEarliestBookingTime());
    assertEquals(BOOKING_TIME_NOON, subject.getLatestBookingTime());
    assertEquals("message", subject.getMessage());
    assertEquals("pickup", subject.getPickupMessage());
    assertEquals("dropoff", subject.getDropOffMessage());

    assertEquals(
      "BookingInfo{contactInfo: ContactInfo{contactPerson: 'Jo Contact', bookingUrl: 'http://booking.otp.org'}, bookingMethods: [CALL_DRIVER], latestBookingTime: 12:00, message: 'message', pickupMessage: 'pickup', dropOffMessage: 'dropoff'}",
      subject.toString()
    );
  }

  @Test
  void testBookingInfoWithMinBookingNotice() {
    Duration minimumBookingNotice = Duration.ofMinutes(45);
    var subject = BookingInfo.of()
      .withBookingMethods(BOOKING_METHODS)
      .withMinimumBookingNotice(minimumBookingNotice)
      .build();

    assertNull(subject.getLatestBookingTime());
    assertEquals(minimumBookingNotice, subject.getMinimumBookingNotice().get());

    assertEquals(
      "BookingInfo{bookingMethods: [CALL_DRIVER], minimumBookingNotice: 45m}",
      subject.toString()
    );
  }
}
