package org.opentripplanner.model.calendar.openinghours;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.BitSet;
import java.util.Objects;
import org.opentripplanner.framework.time.TimeUtils;

/**
 */
public class OpeningHours implements Comparable<OpeningHours>, Serializable {

  private final String periodDescription;
  private final int startTime;
  private final int endTime;
  private final BitSet days;

  /**
   * @param periodDescription Describe the days this opening hours is defined
   */
  OpeningHours(String periodDescription, LocalTime startTime, LocalTime endTime, BitSet days) {
    this.periodDescription = periodDescription;
    this.startTime = startTime.toSecondOfDay();
    this.endTime = endTime.toSecondOfDay();
    this.days = days;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof OpeningHours)) {
      return false;
    }
    final OpeningHours that = (OpeningHours) o;
    // periodDescription is not part of the equals and hashCode, so when deduplicated
    // only the first instance is kept
    return startTime == that.startTime && endTime == that.endTime && days.equals(that.days);
  }

  @Override
  public int hashCode() {
    return Objects.hash(startTime, endTime, days);
  }

  @Override
  public String toString() {
    return (
      periodDescription +
      " " +
      TimeUtils.timeToStrCompact(startTime) +
      "-" +
      TimeUtils.timeToStrCompact(endTime)
    );
  }

  @Override
  public int compareTo(OpeningHours other) {
    return this.startTime == other.startTime
      ? endTime - other.endTime
      : startTime - other.startTime;
  }

  /** return {@code true} if given opening hours is inside this. */
  public boolean contains(OpeningHours other) {
    return this.startTime <= other.startTime && other.endTime <= endTime;
  }

  public boolean isOpen(int day, int secondsSinceMidnight) {
    return (
      days.get(day) && this.startTime <= secondsSinceMidnight && secondsSinceMidnight <= endTime
    );
  }

  public String periodDescription() {
    return periodDescription;
  }

  public long startTime() {
    return startTime;
  }

  public long endTime() {
    return endTime;
  }
}
