package org.opentripplanner.utils.lang;

import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import org.junit.jupiter.api.Test;

class MemEfficientArrayBuilderTest {

  private final DayOfWeek[] WEEKEND = { SATURDAY, SUNDAY };

  @Test
  void size() {
    assertEquals(WEEKEND.length, MemEfficientArrayBuilder.of(WEEKEND).size());
  }

  @Test
  void with() {
    var smallWeekend = MemEfficientArrayBuilder.of(WEEKEND)
      .with(0, DayOfWeek.THURSDAY)
      .with(1, DayOfWeek.FRIDAY)
      .build();
    assertArrayEquals(new DayOfWeek[] { DayOfWeek.THURSDAY, DayOfWeek.FRIDAY }, smallWeekend);
  }

  @Test
  void withOutChange() {
    var array = MemEfficientArrayBuilder.of(WEEKEND).build();
    assertSame(WEEKEND, array);

    array = MemEfficientArrayBuilder.of(WEEKEND).with(0, SATURDAY).build();
    assertSame(WEEKEND, array);

    array = MemEfficientArrayBuilder.of(WEEKEND).with(1, SUNDAY).with(0, SATURDAY).build();
    assertSame(WEEKEND, array);
  }

  @Test
  void getOrOriginal() {
    var array = MemEfficientArrayBuilder.of(WEEKEND).with(1, MONDAY);
    assertEquals(SATURDAY, array.getOrOriginal(0));
    assertEquals(MONDAY, array.getOrOriginal(1));
  }

  @Test
  void original() {
    // Verify that modifications do not change original
    var array = MemEfficientArrayBuilder.of(WEEKEND).with(1, MONDAY);
    assertEquals(SATURDAY, array.original(0));
    assertEquals(SUNDAY, array.original(1));
  }

  @Test
  void isNotModified() {
    var array = MemEfficientArrayBuilder.of(WEEKEND);
    assertTrue(array.isNotModified());

    array.with(0, SATURDAY).with(1, SUNDAY);
    assertTrue(array.isNotModified());

    array.with(0, MONDAY);
    assertFalse(array.isNotModified());
  }

  @Test
  void testBuildWithCandidate() {
    DayOfWeek[] candidate = { TUESDAY, WEDNESDAY };
    var array = MemEfficientArrayBuilder.of(WEEKEND);

    // Without changes, we expect the original to be retuned
    assertSame(WEEKEND, array.build(candidate));

    // Second value set, but not first
    array = MemEfficientArrayBuilder.of(WEEKEND).with(1, WEDNESDAY);
    assertArrayEquals(new DayOfWeek[] { SATURDAY, WEDNESDAY }, array.build(candidate));

    // Same as candidate build
    array = MemEfficientArrayBuilder.of(WEEKEND).with(1, WEDNESDAY).with(0, TUESDAY);
    assertArrayEquals(candidate, array.build(candidate));
  }
}
