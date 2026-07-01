package org.opentripplanner.ext.carpooling;

import java.util.Locale;
import org.opentripplanner.street.geometry.WgsCoordinate;

/**
 * Shared expected-URL formatter for tests that assert the carpool booking URL has been augmented
 * with passenger pickup/dropoff coordinates. Centralised so the production
 * {@code "from_coordinate=lat,lon&to_coordinate=lat,lon"} contract — coordinate precision,
 * parameter names, and ordering — is pinned down in one place that all
 * {@code CarpoolItineraryMapper} tests assert against.
 */
public final class CarpoolBookingUrlTestData {

  private CarpoolBookingUrlTestData() {}

  /**
   * Returns the booking URL that {@code CarpoolItineraryMapper.toBookingInfo} is expected to
   * produce when augmenting {@code baseUrl} with the given carpool boarding ({@code pickup}) and
   * alighting ({@code dropoff}) coordinates. Assumes {@code baseUrl} carries no existing query
   * string and no fragment.
   */
  public static String expectedAugmentedUrl(
    String baseUrl,
    WgsCoordinate pickup,
    WgsCoordinate dropoff
  ) {
    return String.format(
      Locale.ROOT,
      "%s?from_coordinate=%.6f,%.6f&to_coordinate=%.6f,%.6f",
      baseUrl,
      pickup.latitude(),
      pickup.longitude(),
      dropoff.latitude(),
      dropoff.longitude()
    );
  }
}
