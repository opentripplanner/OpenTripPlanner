package org.opentripplanner.netex.support;

import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Utility class with helpers for NeTEx ServiceJourney.
 */
public class ServiceJourneyHelper {

  private ServiceJourneyHelper() {}

  /**
   * Return the elapsed time in second since midnight for a given local time, taking into account
   * the day offset.
   */
  public static int elapsedTimeSinceMidnight(LocalTime time, BigInteger dayOffset) {
    return elapsedTimeSinceMidnight(time, getDayOffset(dayOffset));
  }

  private static int elapsedTimeSinceMidnight(LocalTime time, int dayOffset) {
    Objects.requireNonNull(time);
    return (int) Duration.between(LocalTime.MIDNIGHT, time)
      .plus(Duration.ofDays(dayOffset))
      .toSeconds();
  }

  private static int getDayOffset(BigInteger offset) {
    return offset != null ? offset.intValueExact() : 0;
  }
}
