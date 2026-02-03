package org.opentripplanner.osm.wayproperty;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for parsing human speed values like "5km/h".
 */
class SpeedParser {

  private static final Logger LOG = LoggerFactory.getLogger(SpeedParser.class);
  /**
   * regex courtesy http://wiki.openstreetmap.org/wiki/Key:maxspeed
   * and edited
   */
  private static final Pattern MAX_SPEED_PATTERN = Pattern.compile(
    "^([0-9][.0-9]*)\\s*(kmh|km/h|kmph|kph|mph|knots)?$"
  );

  /**
   * Parses human speed values like "5km/h" into meters per second.
   */
  @Nullable
  static Float getMetersSecondFromSpeed(String speed) {
    Matcher m = MAX_SPEED_PATTERN.matcher(speed.trim());
    if (!m.matches()) {
      return null;
    }

    float originalUnits;
    try {
      originalUnits = (float) Double.parseDouble(m.group(1));
    } catch (NumberFormatException e) {
      LOG.warn("Could not parse max speed {}", m.group(1));
      return null;
    }

    String units = m.group(2);
    if (units == null || units.isEmpty()) {
      units = "kmh";
    }

    // we'll be doing quite a few string comparisons here
    units = units.intern();

    float metersSecond;

    switch (units) {
      case "kmh":
      case "km/h":
      case "kmph":
      case "kph":
        metersSecond = 0.277778f * originalUnits;
        break;
      case "mph":
        metersSecond = 0.446944f * originalUnits;
        break;
      case "knots":
        metersSecond = 0.514444f * originalUnits;
        break;
      default:
        return null;
    }

    return metersSecond;
  }
}
