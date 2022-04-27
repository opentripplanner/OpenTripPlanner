package org.opentripplanner.netex.mapping;

import java.time.Duration;

/**
 * Utility class to help map {@link Duration} class.
 */
class DurationMapper {

  static int mapDurationToSec(Duration duration, int defaultValue) {
    return duration == null ? defaultValue : (int) duration.toSeconds();
  }
}
