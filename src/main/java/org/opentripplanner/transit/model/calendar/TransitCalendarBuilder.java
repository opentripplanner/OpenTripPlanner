package org.opentripplanner.transit.model.calendar;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

public class TransitCalendarBuilder {

  public static final int SUPPORTED_YEARS = 10;
  private ZoneId zoneId = ZoneId.of("Z");
  private LocalDate start;
  private LocalDate end;
  private Duration offset = Duration.ZERO;
  private ZonedDateTime[] startOfDays;
  private int[] offsetNextDaySeconds;

  ZoneId zoneId() {
    return zoneId;
  }

  public TransitCalendarBuilder withZoneId(ZoneId zoneId) {
    this.zoneId = zoneId;
    return this;
  }

  LocalDate periodStart() {
    return start;
  }

  public TransitCalendarBuilder withPeriodStart(LocalDate transitPeriodStart) {
    this.start = transitPeriodStart;
    return this;
  }

  LocalDate periodEnd() {
    return end;
  }

  public TransitCalendarBuilder withPeriodEnd(LocalDate transitPeriodEnd) {
    this.end = transitPeriodEnd;
    return this;
  }

  Duration offset() {
    return offset;
  }

  public TransitCalendarBuilder withOffset(Duration offset) {
    this.offset = offset;
    return this;
  }

  ZonedDateTime[] startOfDays() {
    return startOfDays;
  }

  public int[] offsetNextDaySeconds() {
    return offsetNextDaySeconds;
  }

  void calculateDerivedVariables() {
    assertPeriodIsLimited();

    long t0 = System.currentTimeMillis();
    List<ZonedDateTime> list = new ArrayList<>();
    List<Integer> lengths = new ArrayList<>();

    var time = toStartOfServiceDay(start);
    var end = toStartOfServiceDay(this.end).plusDays(1);

    ZonedDateTime prev = null;

    while (time.isBefore(end)) {
      list.add(time);
      prev = time;
      time = time.plusDays(1);
      lengths.add(between(prev, time));
    }
    this.startOfDays = list.toArray(new ZonedDateTime[0]);
    this.offsetNextDaySeconds = lengths.stream().mapToInt(it -> it).toArray();

    System.out.println("Time: " + (System.currentTimeMillis() - t0) + "ms");
  }

  private void assertPeriodIsLimited() {
    var period = start.until(end);
    if (period.getYears() > SUPPORTED_YEARS && (period.getDays() > 0 || period.getMonths() > 0)) {
      throw new IllegalStateException(
        "Period is too long, more than %d years from %s to %s.".formatted(
            SUPPORTED_YEARS,
            start,
            end
          )
      );
    }
  }

  @Nonnull
  private ZonedDateTime toStartOfServiceDay(LocalDate date) {
    return date.atStartOfDay(zoneId).plus(offset);
  }

  private int between(ZonedDateTime t0, ZonedDateTime t1) {
    return (int) Duration.between(t0, t1).toSeconds();
  }

  TransitCalendar build() {
    calculateDerivedVariables();
    return new TransitCalendar(this);
  }
}
