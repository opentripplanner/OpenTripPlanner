package org.opentripplanner.ext.empiricaldelay.internal.csvinput.calendar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;

public record CalendarRow(
  String serviceId,
  boolean monday,
  boolean tuesday,
  boolean wednesday,
  boolean thursday,
  boolean friday,
  boolean saturday,
  boolean sunday,
  LocalDate startDate,
  LocalDate endDate
) {
  public EnumSet<DayOfWeek> asDayOfWeekSet() {
    var days = EnumSet.noneOf(DayOfWeek.class);
    add(monday, DayOfWeek.MONDAY, days);
    add(tuesday, DayOfWeek.TUESDAY, days);
    add(wednesday, DayOfWeek.WEDNESDAY, days);
    add(thursday, DayOfWeek.THURSDAY, days);
    add(friday, DayOfWeek.FRIDAY, days);
    add(saturday, DayOfWeek.SATURDAY, days);
    add(sunday, DayOfWeek.SUNDAY, days);
    return days;
  }

  private static void add(boolean add, DayOfWeek dayOfWeek, EnumSet<DayOfWeek> target) {
    if (add) {
      target.add(dayOfWeek);
    }
  }
}
