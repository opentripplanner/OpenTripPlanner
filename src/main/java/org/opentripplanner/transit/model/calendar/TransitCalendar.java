package org.opentripplanner.transit.model.calendar;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 *
 */
public class TransitCalendar {

  private final ZoneId zoneId;
  private final LocalDate start;
  private final LocalDate end;
  private final Duration offset;
  private final ZonedDateTime[] startOfDays;
  private final int[] dayLengthSeconds;

  TransitCalendar(TransitCalendarBuilder builder) {
    this.zoneId = Objects.requireNonNull(builder.zoneId());
    this.start = Objects.requireNonNull(builder.periodStart());
    this.end = Objects.requireNonNull(builder.periodEnd());
    this.offset = Objects.requireNonNull(builder.offset());
    this.startOfDays = Objects.requireNonNull(builder.startOfDays());
    this.dayLengthSeconds = Objects.requireNonNull(builder.offsetNextDaySeconds());
  }

  public static TransitCalendarBuilder of() {
    return new TransitCalendarBuilder();
  }

  public ZoneId zoneId() {
    return zoneId;
  }

  public LocalDate start() {
    return start;
  }

  public LocalDate end() {
    return end;
  }

  public Duration offset() {
    return offset;
  }

  public ZonedDateTime time(int day, int time) {
    return startOfDays[day].plusSeconds(time);
  }

  public TransitTime time(ZonedDateTime time) {
    int days = (int) ChronoUnit.DAYS.between(startOfDays[0], time);
    int seconds = (int) ChronoUnit.SECONDS.between(startOfDays[days], time);

    return new TransitTime.Builder(this).withDay(days).withTime(seconds).build();
  }

  public int dayLengthSeconds(int day) {
    return dayLengthSeconds[day];
  }
}
