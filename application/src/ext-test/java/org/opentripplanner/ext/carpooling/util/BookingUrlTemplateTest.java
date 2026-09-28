package org.opentripplanner.ext.carpooling.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolBookingUrlTestData.expandedCoordinate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.street.geometry.WgsCoordinate;

class BookingUrlTemplateTest {

  private static final WgsCoordinate PICKUP = new WgsCoordinate(59.910000, 10.750000);
  private static final WgsCoordinate DROPOFF = new WgsCoordinate(59.920000, 10.760000);

  private static final String PICKUP_COORD = expandedCoordinate(PICKUP);
  private static final String DROPOFF_COORD = expandedCoordinate(DROPOFF);

  @Test
  void rendersCoordinateAsLatitudeCommaLongitudeAtSixDecimals() {
    assertEquals(
      "https://book.example.com/?pickup=59.910000,10.750000",
      expand("https://book.example.com/?pickup={from}")
    );
  }

  /** A provider wanting only the pickup publishes only {@code {from}}; the rest is untouched. */
  @Test
  void leavesTheRestOfTheUrlAlone() {
    assertEquals(
      "https://book.example.com/trip/42?pickup=" + PICKUP_COORD + "&ref=foo",
      expand("https://book.example.com/trip/42?pickup={from}&ref=foo")
    );
  }

  /** Placeholders are expanded wherever they appear, and at every occurrence. */
  @Test
  void expandsEveryComponentAndEveryOccurrence() {
    assertEquals(
      "https://book.example.com/book/" +
        PICKUP_COORD +
        "/to/" +
        DROPOFF_COORD +
        "?pickup=" +
        PICKUP_COORD +
        "#at/" +
        DROPOFF_COORD,
      expand("https://book.example.com/book/{from}/to/{to}?pickup={from}#at/{to}")
    );
  }

  @Test
  void passesAUrlWithoutPlaceholdersThroughUnchanged() {
    var url = "https://book.example.com/trip/42?ref=foo#bookform";

    assertEquals(url, expand(url));
    assertTrue(BookingUrlTemplate.isUsable(url));
  }

  @Test
  void acceptsATemplate() {
    assertTrue(BookingUrlTemplate.isUsable("https://book.example.com/book/{from}?dropoff={to}"));
  }

  /**
   * A misspelled {@code {From}} is not expanded, and its surviving braces are not legal URI
   * characters — as a stray space is not.
   */
  @ParameterizedTest
  @ValueSource(
    strings = { "https://book.example.com/trip/42?pickup={From}", "https://book.example.com/a b" }
  )
  void rejectsWhatIsNotAUri(String urlTemplate) {
    assertFalse(BookingUrlTemplate.isUsable(urlTemplate), urlTemplate);
  }

  private static String expand(String urlTemplate) {
    return BookingUrlTemplate.expand(urlTemplate, PICKUP, DROPOFF);
  }
}
