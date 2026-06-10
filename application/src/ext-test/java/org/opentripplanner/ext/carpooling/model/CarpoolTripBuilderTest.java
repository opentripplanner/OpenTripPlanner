package org.opentripplanner.ext.carpooling.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_EAST;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createSimpleTrip;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createStopAt;

import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.organization.ContactInfo;

public class CarpoolTripBuilderTest {

  @Test
  void buildFromValues_fromId_buildToCorrectValues() {
    var startTime = ZonedDateTime.now();
    var endTime = ZonedDateTime.now().plusMinutes(45);
    var stop = createStopAt(OSLO_EAST);

    var builder = new CarpoolTripBuilder(new FeedScopedId("feed", "id"));
    var trip = builder
      .withTotalCapacity(2)
      .withProvider("UNIT")
      .withStartTime(startTime)
      .withEndTime(endTime)
      .withStops(List.of(stop))
      .buildFromValues();

    assertEquals(2, trip.totalCapacity());
    assertEquals("UNIT", trip.provider());
    assertEquals(startTime, trip.startTime());
    assertEquals(endTime, trip.endTime());
    assertEquals(stop, trip.stops().getFirst());
  }

  @Test
  void buildFromValues_fromOriginal_buildToCorrectValues() {
    var original = createSimpleTrip(OSLO_EAST, OSLO_NORTH);

    var builder = new CarpoolTripBuilder(original);
    var trip = builder.buildFromValues();

    assertEquals(original.totalCapacity(), trip.totalCapacity());
    assertEquals(original.provider(), trip.provider());
    assertEquals(original.startTime(), trip.startTime());
    assertEquals(original.endTime(), trip.endTime());
    assertEquals(original.stops().getFirst(), trip.stops().getFirst());
    assertEquals(original.stops().getLast(), trip.stops().getLast());
  }

  @Test
  void buildFromValues_withPublicContactInformation_storesContactInfo() {
    var contact = ContactInfo.of()
      .withPhoneNumber("+4712345678")
      .withBookingUrl("https://example.com/book")
      .build();
    var stop = createStopAt(1, OSLO_EAST);

    var trip = new CarpoolTripBuilder(new FeedScopedId("feed", "contact-test"))
      .withStartTime(ZonedDateTime.now())
      .withEndTime(ZonedDateTime.now().plusMinutes(30))
      .withStops(List.of(stop))
      .withPublicContactInformation(contact)
      .buildFromValues();

    assertNotNull(trip.publicContactInformation());
    assertEquals("+4712345678", trip.publicContactInformation().getPhoneNumber());
    assertEquals("https://example.com/book", trip.publicContactInformation().getBookingUrl());
  }

  @Test
  void buildFromValues_withNullableContactFields_allowsNulls() {
    var contactPhoneOnly = ContactInfo.of().withPhoneNumber("+4712345678").build();
    var contactUrlOnly = ContactInfo.of().withBookingUrl("https://example.com/book").build();
    var stop = createStopAt(1, OSLO_EAST);

    var tripWithPhone = new CarpoolTripBuilder(new FeedScopedId("feed", "phone-only"))
      .withStartTime(ZonedDateTime.now())
      .withEndTime(ZonedDateTime.now().plusMinutes(30))
      .withStops(List.of(stop))
      .withPublicContactInformation(contactPhoneOnly)
      .buildFromValues();

    assertEquals("+4712345678", tripWithPhone.publicContactInformation().getPhoneNumber());
    assertNull(tripWithPhone.publicContactInformation().getBookingUrl());

    var tripWithUrl = new CarpoolTripBuilder(new FeedScopedId("feed", "url-only"))
      .withStartTime(ZonedDateTime.now())
      .withEndTime(ZonedDateTime.now().plusMinutes(30))
      .withStops(List.of(stop))
      .withPublicContactInformation(contactUrlOnly)
      .buildFromValues();

    assertNull(tripWithUrl.publicContactInformation().getPhoneNumber());
    assertEquals(
      "https://example.com/book",
      tripWithUrl.publicContactInformation().getBookingUrl()
    );
  }

  @Test
  void buildFromValues_withoutPublicContactInformation_defaultsToNull() {
    var stop = createStopAt(1, OSLO_EAST);

    var trip = new CarpoolTripBuilder(new FeedScopedId("feed", "no-contact"))
      .withStartTime(ZonedDateTime.now())
      .withEndTime(ZonedDateTime.now().plusMinutes(30))
      .withStops(List.of(stop))
      .buildFromValues();

    assertNull(trip.publicContactInformation());
  }
}
