package org.opentripplanner.transit.model.calendar;

import java.time.ZonedDateTime;
import org.opentripplanner.framework.time.DurationUtils;

/**
 * Otp store timetables for a given trip pattern per OPERATION DAY. The operation day internally in
 * OTP is .... TODO RTM - Fixed offset like 04:00 - Apply to all patterns - Trips are grouped by the
 * arrival time (or departure time if arrival time does not exist) at the first stop position in
 * pattern. - There is a limited number of OperationDay, one for each day between the start and end
 * of the transit period.
 */
public record OperatingDay(int dayIndex, ZonedDateTime startTime, int lengthSeconds) {
  // TODO RTM

  public ZonedDateTime toTime(int time) {
    return startTime.plusSeconds(time);
  }

  @Override
  public String toString() {
    // TODO RTM - Return "OperationDay{#5 2022-01-17 04:30 24h)"
    return (
      "OperationDay{" +
      "#" +
      dayIndex +
      // " " + TimeUtils.dateAndTimeToStrCompact(startTime) +
      " " +
      DurationUtils.durationToStr(lengthSeconds) +
      '}'
    );
  }
}
