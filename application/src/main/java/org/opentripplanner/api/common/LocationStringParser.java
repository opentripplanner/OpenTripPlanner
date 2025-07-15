package org.opentripplanner.api.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * This class is used by the GTFS GraphQL API to parse strings representing the from and to places for a
 * search. These strings can contain a user-specified name for the location, as well as a latitude
 * and longitude or a stop ID. We'd rather not be parsing multiple data items out of a single
 * string, and avoid doing so in newer API queries, but this is how the old queries work and we're
 * keeping this parsing logic so we can keep providing that legacy API.
 * <p>
 * These from/to strings are in the following format: An optional place name followed by two colons,
 * then either a latitude and longitude or a feed-scoped ID for a stop or stop collection
 * (station).
 * <p>
 * See LocationStringParserTest for examples of valid strings.
 */
public class LocationStringParser {

  // Pattern for matching a latitude or longitude string: an optional '-' character followed
  // by one or more digits, and an optional (decimal point followed by one or more digits).
  private static final String DOUBLE_PATTERN = "-{0,1}\\d+(\\.\\d+){0,1}";

  // The pattern for a latitude and longitude together. For some reason this is designed to
  // ignore any number of non-digit characters except +/- signs at the beginning of the
  // string. So it ignores any number of non-(digit or sign or decimal point).
  // Regex has been rewritten following https://bugs.openjdk.java.net/browse/JDK-8189343
  // from "[^[\\d&&[-|+|.]]]*(" to "[\\D&&[^-+.]]*("
  private static final Pattern LAT_LON_PATTERN = Pattern.compile(
    "[\\D&&[^-+.]]*(" + DOUBLE_PATTERN + ")(\\s*,\\s*|\\s+)(" + DOUBLE_PATTERN + ")\\D*"
  );

  /**
   * Creates the GenericLocation by parsing a "name::place" string, where "place" is a geographic
   * coordinate string (latitude,longitude) or a feed scoped ID (feedId:stopId).
   */
  public static GenericLocation fromOldStyleString(String input) {
    String name = null;
    String place = input;
    if (input.contains("::")) {
      String[] parts = input.split("::", 2);
      name = parts[0];
      place = parts[1];
    }
    return getGenericLocation(name, place);
  }

  /**
   * Construct from two Strings, a label and a place. The label is an arbitrary user specified name
   * for the location that can pass through to the routing response unchanged. The place contains
   * latitude and longitude or a stop ID.
   */
  public static GenericLocation getGenericLocation(String label, String place) {
    if (place == null) {
      return null;
    }

    Double lat = null;
    Double lon = null;
    FeedScopedId placeId = null;

    Matcher matcher = LAT_LON_PATTERN.matcher(place);
    if (matcher.find()) {
      lat = Double.parseDouble(matcher.group(1));
      lon = Double.parseDouble(matcher.group(4));
    } else if (FeedScopedId.isValidString(place)) {
      placeId = FeedScopedId.parse(place);
    }
    return new GenericLocation(label, placeId, lat, lon);
  }
}
