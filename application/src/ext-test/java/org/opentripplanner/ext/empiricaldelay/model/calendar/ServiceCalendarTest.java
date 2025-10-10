package org.opentripplanner.ext.empiricaldelay.model.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.asserts.AssertEqualsAndHashCode;

class ServiceCalendarTest {

  private static final LocalDate START = LocalDate.of(2025, Month.SEPTEMBER, 2);
  private static final LocalDate END = LocalDate.of(2025, Month.SEPTEMBER, 30);
  private static final String SID = "sid";

  private final ServiceCalendarPeriod subject = new ServiceCalendarPeriod(SID, START, END);

  @Test
  void accept() {
    assertTrue(subject.accept(START));
    assertTrue(subject.accept(START.plusDays(10)));
    assertTrue(subject.accept(END));

    assertFalse(subject.accept(START.minusDays(1)));
    assertFalse(subject.accept(END.plusDays(1)));
  }

  @Test
  void serviceId() {
    assertEquals(SID, subject.serviceId());
  }

  @Test
  void testEqualsAndHashCode() {
    AssertEqualsAndHashCode.verify(subject)
      .differentFrom(
        new ServiceCalendarPeriod("xsid", START, END),
        new ServiceCalendarPeriod(SID, START.plusDays(1), END),
        new ServiceCalendarPeriod(SID, START, END.plusDays(1))
      )
      .sameAs(new ServiceCalendarPeriod(SID, START, END));
  }

  @Test
  void testToString() {
    assertEquals(
      "ServiceCalendarPeriod{serviceId: 'sid', start: 2025-09-02, end: 2025-09-30}",
      subject.toString()
    );
  }

  @Test
  void isSerializable() {
    assertTrue(subject instanceof Serializable);
  }
}
