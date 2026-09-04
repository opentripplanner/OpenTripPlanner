package org.opentripplanner.ext.carpooling;

import java.util.Locale;
import org.opentripplanner.street.geometry.WgsCoordinate;

/**
 * Shared booking-URL template and its expected expansion, for tests asserting that the carpool
 * booking URL has had the passenger's pickup/dropoff coordinates expanded into it. Centralised so
 * the production contract — the {@code {from}}/{@code {to}} placeholder spelling and the
 * {@code "latitude,longitude"} rendering at six decimals — is pinned down in one place that all
 * {@code CarpoolItineraryMapper} tests assert against.
 * <p>
 * The query parameter names carrying the placeholders ({@code from_coordinate},
 * {@code to_coordinate}) are the provider's own choice and form no part of OTP's contract; they
 * are here only to make the template realistic.
 */
public final class CarpoolBookingUrlTestData {

  private CarpoolBookingUrlTestData() {}

  /**
   * Returns {@code baseUrl} with a query string carrying both placeholders — the shape of booking
   * URL a carpool provider publishes in order to receive the passenger's coordinates.
   */
  public static String bookingUrlTemplate(String baseUrl) {
    return baseUrl + "?from_coordinate={from}&to_coordinate={to}";
  }

  /**
   * Returns the {@code "latitude,longitude"} rendering that a {@code {from}} or {@code {to}}
   * placeholder is expected to expand to. The single place test code spells this format out, so
   * that a change of precision has one place to be updated on the test side.
   */
  public static String expandedCoordinate(WgsCoordinate coordinate) {
    return String.format(Locale.ROOT, "%.6f,%.6f", coordinate.latitude(), coordinate.longitude());
  }

  /**
   * Returns the URL that {@code CarpoolItineraryMapper.toBookingInfo} is expected to produce from
   * {@link #bookingUrlTemplate(String)} for the given carpool boarding ({@code pickup}) and
   * alighting ({@code dropoff}) coordinates.
   */
  public static String expectedExpandedUrl(
    String baseUrl,
    WgsCoordinate pickup,
    WgsCoordinate dropoff
  ) {
    return (
      baseUrl +
      "?from_coordinate=" +
      expandedCoordinate(pickup) +
      "&to_coordinate=" +
      expandedCoordinate(dropoff)
    );
  }
}
