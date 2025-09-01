package org.opentripplanner.ext.empiricaldelay.model.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EmpiricalDelayCalendarTest {

  /** Tuesday 2. September */
  private static final LocalDate START_WEEKDAYS_SERVICE = LocalDate.of(2025, Month.SEPTEMBER, 2);
  /** Tuesday 30. September */
  private static final LocalDate END_WEEKDAYS_SERVICE = LocalDate.of(2025, Month.SEPTEMBER, 30);
  /** Monday 15. September */
  private static final LocalDate START_SATURDAY_SERVICE = LocalDate.of(2025, Month.SEPTEMBER, 15);
  /** Monday 13. October */
  private static final LocalDate END_SATURDAY_SERVICE = LocalDate.of(2025, Month.OCTOBER, 13);
  /** Saturday 20. September */
  private static final LocalDate FIRST_SATURDAY_IN_SAT_SERVICE = LocalDate.of(
    2025,
    Month.SEPTEMBER,
    20
  );
  /** Saturday 11. October */
  private static final LocalDate LAST_SATURDAY_IN_SAT_SERVICE = LocalDate.of(
    2025,
    Month.OCTOBER,
    11
  );

  private static final Set<DayOfWeek> WEEKDAYS = Set.of(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY
  );
  private static final Set<DayOfWeek> SATURDAY = Set.of(DayOfWeek.SATURDAY);
  private static final String MON_FRI_SERVICE_ID = "MON-FRI";
  private static final String SATURDAY_SERICE_ID = "SATURDAY";

  private EmpiricalDelayCalendar subject = EmpiricalDelayCalendar.of()
    // Note! Start and end dates match MON-FRI (Start and stop are Tuesdays)
    .with(MON_FRI_SERVICE_ID, WEEKDAYS, START_WEEKDAYS_SERVICE, END_WEEKDAYS_SERVICE)
    // Note! Start and end dates do not match SATURDAY (Start and stop are Tuesdays)
    .with(SATURDAY_SERICE_ID, SATURDAY, START_SATURDAY_SERVICE, END_SATURDAY_SERVICE)
    .build();

  @Test
  void findServiceId() {
    // Assert is right for all days a week - all cases are within both service calendars
    var monday = START_SATURDAY_SERVICE;

    assertEquals(MON_FRI_SERVICE_ID, subject.findServiceId(monday).get());
    assertEquals(MON_FRI_SERVICE_ID, subject.findServiceId(monday.plusDays(1)).get());
    assertEquals(MON_FRI_SERVICE_ID, subject.findServiceId(monday.plusDays(2)).get());
    assertEquals(MON_FRI_SERVICE_ID, subject.findServiceId(monday.plusDays(3)).get());
    assertEquals(MON_FRI_SERVICE_ID, subject.findServiceId(monday.plusDays(4)).get());
    // Saturday
    assertEquals(SATURDAY_SERICE_ID, subject.findServiceId(monday.plusDays(5)).get());
    // Sunday - no service defined
    assertEquals(Optional.empty(), subject.findServiceId(monday.plusDays(6)));

    // Assert start of period
    assertEquals(MON_FRI_SERVICE_ID, subject.findServiceId(START_WEEKDAYS_SERVICE).get());

    assertEquals(Optional.empty(), subject.findServiceId(START_WEEKDAYS_SERVICE.minusDays(1)));
    assertEquals(SATURDAY_SERICE_ID, subject.findServiceId(FIRST_SATURDAY_IN_SAT_SERVICE).get());
    assertEquals(
      MON_FRI_SERVICE_ID,
      subject.findServiceId(FIRST_SATURDAY_IN_SAT_SERVICE.minusDays(1)).get()
    );

    // Assert end of period
    assertEquals(MON_FRI_SERVICE_ID, subject.findServiceId(END_WEEKDAYS_SERVICE).get());
    assertEquals(Optional.empty(), subject.findServiceId(END_WEEKDAYS_SERVICE.plusDays(1)));
    assertEquals(SATURDAY_SERICE_ID, subject.findServiceId(LAST_SATURDAY_IN_SAT_SERVICE).get());
    assertEquals(Optional.empty(), subject.findServiceId(LAST_SATURDAY_IN_SAT_SERVICE.plusDays(1)));
  }

  @Test
  void listServiceIds() {
    assertEquals(List.of(MON_FRI_SERVICE_ID, SATURDAY_SERICE_ID), subject.listServiceIds());
  }

  @Test
  void isSerializable() {
    assertTrue(subject instanceof Serializable);
  }
}
