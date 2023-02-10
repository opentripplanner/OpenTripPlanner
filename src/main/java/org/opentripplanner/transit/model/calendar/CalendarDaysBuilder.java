package org.opentripplanner.transit.model.calendar;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

public class CalendarDaysBuilder {

  public static final int SUPPORTED_YEARS = 10;
  private ZoneId zoneId;

  private LocalDate start;
  private LocalDate end;
  private Duration timeOffset;
  private OperatingDay[] operatingDays;
  private int[] offsetNextDaySeconds;

  CalendarDaysBuilder(ZoneId zoneId, LocalDate start, LocalDate end, Duration timeOffset) {
    this.zoneId = zoneId;
    this.start = start;
    this.end = end;
    this.timeOffset = timeOffset;
  }

  ZoneId zoneId() {
    return zoneId;
  }

  public CalendarDaysBuilder withZoneId(ZoneId zoneId) {
    this.zoneId = zoneId;
    return this;
  }

  LocalDate periodStart() {
    return start;
  }

  public CalendarDaysBuilder withPeriodStart(LocalDate transitPeriodStart) {
    this.start = transitPeriodStart;
    return this;
  }

  LocalDate periodEnd() {
    return end;
  }

  public CalendarDaysBuilder withPeriodEnd(LocalDate transitPeriodEnd) {
    this.end = transitPeriodEnd;
    return this;
  }

  Duration timeOffset() {
    return timeOffset;
  }

  public CalendarDaysBuilder withOffset(Duration timeOffset) {
    this.timeOffset = timeOffset;
    return this;
  }

  OperatingDay[] operatingDays() {
    return operatingDays;
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

    ZonedDateTime prev;

    while (time.isBefore(end)) {
      list.add(time);
      prev = time;
      time = time.plusDays(1);
      lengths.add(between(prev, time));
    }

    // TODO RTM -
    //this.startOfDays = list.toArray(new ZonedDateTime[0]);
    this.offsetNextDaySeconds = lengths.stream().mapToInt(it -> it).toArray();
    this.operatingDays = new OperatingDay[list.size()];
    for (int i = 0; i < list.size(); i++) {
      this.operatingDays[i] = new OperatingDay(i, list.get(i), lengths.get(i));
    }

    System.out.println("Time: " + (System.currentTimeMillis() - t0) + "ms");
  }

  private void assertPeriodIsLimited() {
    var period = start.until(end);
    if (period.isNegative()) {
      throw new IllegalArgumentException("end date " + end + " must be after start date " + start);
    }
    if (period.getYears() > SUPPORTED_YEARS && (period.getDays() > 0 || period.getMonths() > 0)) {
      throw new IllegalArgumentException(
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
    return date.atStartOfDay(zoneId).plus(timeOffset);
  }

  private int between(ZonedDateTime t0, ZonedDateTime t1) {
    return (int) Duration.between(t0, t1).toSeconds();
  }

  public CalendarDays build() {
    calculateDerivedVariables();
    return new CalendarDays(this);
  }
}
