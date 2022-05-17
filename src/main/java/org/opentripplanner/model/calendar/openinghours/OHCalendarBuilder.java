package org.opentripplanner.model.calendar.openinghours;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import org.opentripplanner.routing.trippattern.Deduplicator;

public class OHCalendarBuilder {

  private final Deduplicator deduplicator;
  private final LocalDate startOfPeriod;
  private final LocalDate endOfPeriod;
  private final int daysInPeriod;
  private final ZoneId zoneId;
  private final List<OpeningHours> openingHours = new ArrayList<>();

  public OHCalendarBuilder(
    Deduplicator deduplicator,
    LocalDate startOfPeriod,
    int daysInPeriod,
    ZoneId zoneId
  ) {
    this.deduplicator = deduplicator;
    this.startOfPeriod = startOfPeriod;
    this.endOfPeriod = startOfPeriod.plusDays(daysInPeriod);
    this.daysInPeriod = daysInPeriod;
    this.zoneId = zoneId;
  }

  public OpeningHoursBuilder openingHours(
    String periodDescription,
    LocalTime startTime,
    LocalTime endTime
  ) {
    return new OpeningHoursBuilder(periodDescription, startTime, endTime);
  }

  public OHCalendar build() {
    // We sort the opening hours for the deduplicator to work a little better and to simplify
    // the check can Enter/Exit later. Even if the opening hours are not on the same dates they
    // will still be sorted in the right order after day filtering
    Collections.sort(openingHours);
    return new OHCalendar(
      zoneId,
      deduplicator.deduplicateImmutableList(OpeningHours.class, openingHours)
    );
  }

  public class OpeningHoursBuilder {

    private final String periodDescription;
    private final LocalTime startTime;
    private final LocalTime endTime;

    private final BitSet openingDays = new BitSet(daysInPeriod);

    public OpeningHoursBuilder(String periodDescription, LocalTime startTime, LocalTime endTime) {
      this.periodDescription = periodDescription;
      this.startTime = startTime;
      this.endTime = endTime;
    }

    public OpeningHoursBuilder on(LocalDate date) {
      if (date.isBefore(startOfPeriod) || date.isAfter(endOfPeriod)) {
        return this;
      }
      openingDays.set((int) ChronoUnit.DAYS.between(startOfPeriod, date));
      return this;
    }

    public OpeningHoursBuilder on(DayOfWeek dayOfWeek) {
      // This counts how many days there are in between the startOfPeriod and
      // when the specified dayOfWeek occurs for the first time. Maybe there is a cleaner way to do this.
      int rawWeekDayDifference = dayOfWeek.compareTo(startOfPeriod.getDayOfWeek());
      int firstOccurrenceDaysFromStart = rawWeekDayDifference >= 0
        ? rawWeekDayDifference
        : 7 - Math.abs(rawWeekDayDifference);

      for (int i = firstOccurrenceDaysFromStart; i < daysInPeriod; i += 7) {
        openingDays.set(i);
      }
      return this;
    }

    public OHCalendarBuilder add() {
      var days = deduplicator.deduplicateBitSet(openingDays);
      var hours = deduplicator.deduplicateObject(
        OpeningHours.class,
        new OpeningHours(periodDescription, startTime, endTime, days)
      );
      openingHours.add(hours);
      return OHCalendarBuilder.this;
    }
  }
}
