package org.opentripplanner.ext.empiricaldelay.internal.csvinput.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class CalendarRowTest {

  private static final LocalDate START = LocalDate.of(2025, 1, 1);
  private static final LocalDate END = LocalDate.of(2025, 12, 31);

  private final CalendarRow MON = new CalendarRow(
    "MON",
    true,
    false,
    false,
    false,
    false,
    false,
    false,
    START,
    END
  );
  private final CalendarRow TUE = new CalendarRow(
    "MON",
    false,
    true,
    false,
    false,
    false,
    false,
    false,
    START,
    END
  );
  private final CalendarRow WED = new CalendarRow(
    "MON",
    false,
    false,
    true,
    false,
    false,
    false,
    false,
    START,
    END
  );
  private final CalendarRow THR = new CalendarRow(
    "MON",
    false,
    false,
    false,
    true,
    false,
    false,
    false,
    START,
    END
  );
  private final CalendarRow FRI = new CalendarRow(
    "MON",
    false,
    false,
    false,
    false,
    true,
    false,
    false,
    START,
    END
  );
  private final CalendarRow SAT = new CalendarRow(
    "MON",
    false,
    false,
    false,
    false,
    false,
    true,
    false,
    START,
    END
  );
  private final CalendarRow SUN = new CalendarRow(
    "MON",
    false,
    false,
    false,
    false,
    false,
    false,
    true,
    START,
    END
  );

  @Test
  void serviceId() {
    assertEquals("MON", MON.serviceId());
  }

  @Test
  void startDate() {
    assertEquals(START, MON.startDate());
  }

  @Test
  void endDate() {
    assertEquals(END, MON.endDate());
  }

  @Test
  void testToString() {
    assertEquals(
      "CalendarRow[serviceId=MON, monday=true, tuesday=false, wednesday=false, thursday=false, " +
      "friday=false, saturday=false, sunday=false, startDate=2025-01-01, endDate=2025-12-31]",
      MON.toString()
    );
  }

  @Test
  void asDayOfWeekSet() {
    assertEquals(EnumSet.of(DayOfWeek.MONDAY), MON.asDayOfWeekSet());
    assertEquals(EnumSet.of(DayOfWeek.TUESDAY), TUE.asDayOfWeekSet());
    assertEquals(EnumSet.of(DayOfWeek.WEDNESDAY), WED.asDayOfWeekSet());
    assertEquals(EnumSet.of(DayOfWeek.THURSDAY), THR.asDayOfWeekSet());
    assertEquals(EnumSet.of(DayOfWeek.FRIDAY), FRI.asDayOfWeekSet());
    assertEquals(EnumSet.of(DayOfWeek.SATURDAY), SAT.asDayOfWeekSet());
    assertEquals(EnumSet.of(DayOfWeek.SUNDAY), SUN.asDayOfWeekSet());
  }
}
