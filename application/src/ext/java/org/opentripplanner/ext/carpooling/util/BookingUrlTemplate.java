package org.opentripplanner.ext.carpooling.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import org.opentripplanner.street.geometry.WgsCoordinate;

/**
 * The booking URL a carpool provider publishes, holding {@code {from}} and {@code {to}}
 * placeholders for the passenger's carpool boarding and alighting points. Each is expanded to
 * {@code "latitude,longitude"} at six decimals, wherever in the URL it appears. Both are
 * optional: a URL holding neither is returned unchanged.
 */
public final class BookingUrlTemplate {

  private static final String FROM_PLACEHOLDER = "{from}";
  private static final String TO_PLACEHOLDER = "{to}";

  /** Stand-in for validating a template before a passenger's coordinates are known. */
  private static final WgsCoordinate PROBE = new WgsCoordinate(-90, -180);

  private BookingUrlTemplate() {}

  /**
   * Checks that the template expands to a parseable URI. Curly braces are not legal URI
   * characters, so it is the expansion that is parsed, not the template as published.
   */
  public static boolean isUsable(String urlTemplate) {
    try {
      new URI(expand(urlTemplate, PROBE, PROBE));
      return true;
    } catch (URISyntaxException e) {
      return false;
    }
  }

  public static String expand(String urlTemplate, WgsCoordinate from, WgsCoordinate to) {
    return urlTemplate
      .replace(FROM_PLACEHOLDER, formatCoordinate(from))
      .replace(TO_PLACEHOLDER, formatCoordinate(to));
  }

  private static String formatCoordinate(WgsCoordinate coordinate) {
    return String.format(Locale.ROOT, "%.6f,%.6f", coordinate.latitude(), coordinate.longitude());
  }
}
