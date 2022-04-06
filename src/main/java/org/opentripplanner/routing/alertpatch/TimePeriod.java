package org.opentripplanner.routing.alertpatch;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Represents a period of time, in terms of seconds in [start, end)
 *
 * @author novalis
 */
public class TimePeriod {

  // TODO OTP2 - This class is used both for internal and external API representation,
  //           - a external API version should be created to decouple the internal model
  //           - from any API usage.

  public static final long OPEN_ENDED = Long.MAX_VALUE;

  @JsonSerialize
  public long startTime;

  @JsonSerialize
  public long endTime;

  public TimePeriod(long start, long end) {
    this.startTime = start;
    this.endTime = end;
  }

  public TimePeriod() {}

  public int hashCode() {
    return (int) ((startTime & 0x7fff) + (endTime & 0x7fff));
  }

  public boolean equals(Object o) {
    if (!(o instanceof TimePeriod)) {
      return false;
    }
    TimePeriod other = (TimePeriod) o;
    return other.startTime == startTime && other.endTime == endTime;
  }
}
