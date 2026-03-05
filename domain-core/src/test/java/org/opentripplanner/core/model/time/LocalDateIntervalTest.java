package org.opentripplanner.core.model.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

public class LocalDateIntervalTest {

  private final LocalDate d0 = LocalDate.of(2020, 1, 1);
  private final LocalDate d1 = LocalDate.of(2020, 1, 7);
  private final LocalDate d2 = LocalDate.of(2020, 1, 15);
  private final LocalDate d3 = LocalDate.of(2020, 2, 1);
  private final LocalDate d4 = LocalDate.of(2020, 2, 7);

  @Test
  public void constructorFailsIfEndIsBeforeTheStart() {
    assertThrows(IllegalArgumentException.class, () -> new LocalDateInterval(d1.plusDays(1), d1));
  }

  @Test
  public void unlimited() {
    LocalDateInterval u = LocalDateInterval.unbounded();
    assertTrue(u.include(LocalDate.MIN));
    assertTrue(u.include(LocalDate.MAX));
  }

  @Test
  public void isUnbounded() {
    assertTrue(LocalDateInterval.unbounded().isUnbounded());
    assertFalse(new LocalDateInterval(null, d2).isUnbounded());
    assertFalse(new LocalDateInterval(d2, null).isUnbounded());
  }

  @Test
  public void getStart() {
    assertEquals(d1, new LocalDateInterval(d1, d2).getStart());
    assertEquals(LocalDate.MIN, LocalDateInterval.unbounded().getStart());
  }

  @Test
  public void getEnd() {
    assertEquals(d2, new LocalDateInterval(d1, d2).getEnd());
    assertEquals(LocalDate.MAX, LocalDateInterval.unbounded().getEnd());
  }

  @Test
  public void overlap() {
    LocalDateInterval subject = new LocalDateInterval(d1, d2);
    LocalDateInterval other;

    // First day overlap
    other = new LocalDateInterval(d0, d1);
    assertTrue(subject.overlap(other), subject + " should overlap " + other);

    // Last day overlap
    other = new LocalDateInterval(d2, d3);
    assertTrue(subject.overlap(other), subject + " should overlap " + other);

    // Same periods overlap
    other = new LocalDateInterval(d1, d2);
    assertTrue(subject.overlap(other), subject + " should overlap " + other);

    // Small period overlap part of large
    other = new LocalDateInterval(d0, d4);
    assertTrue(subject.overlap(other), subject + " should overlap " + other);

    // Period ending day before, do NOT overlap
    other = new LocalDateInterval(d0, d1.minusDays(1));
    assertFalse(subject.overlap(other), subject + " should not overlap " + other);

    // Period start day after, do NOT overlap
    other = new LocalDateInterval(d2.plusDays(1), d3);
    assertFalse(subject.overlap(other), subject + " should not overlap " + other);

    // Period overlap with unlimited
    LocalDateInterval unlimited = LocalDateInterval.unbounded();
    assertTrue(subject.overlap(unlimited), subject + " should overlap unlimited");

    // Unlimited overlap with unlimited
    assertTrue(unlimited.overlap(unlimited), "Unlimited should overlap it self");
  }

  @Test
  public void intersection() {
    LocalDateInterval subject = new LocalDateInterval(d1, d2);

    // Intersection of subject and subject -> subject
    assertEquals(subject, subject.intersection(subject));

    // First day in common
    assertEquals(
      new LocalDateInterval(d1, d1),
      subject.intersection(new LocalDateInterval(d0, d1))
    );

    // Last day in common
    assertEquals(
      new LocalDateInterval(d2, d2),
      subject.intersection(new LocalDateInterval(d2, d3))
    );

    // The intersection of subject and unlimited -> subject
    assertEquals(subject, subject.intersection(LocalDateInterval.unbounded()));
  }

  @Test
  public void intersectionFailsIfAUnionDoNotExist() {
    assertThrows(IllegalArgumentException.class, () ->
      new LocalDateInterval(d0, d1).intersection(new LocalDateInterval(d1.plusDays(1), d2))
    );
  }

  @Test
  public void include() {
    LocalDateInterval subject = new LocalDateInterval(d1, d3);

    assertFalse(subject.include(d0));
    assertTrue(subject.include(d1));
    assertTrue(subject.include(d2));
    assertTrue(subject.include(d3));
    assertFalse(subject.include(d4));
  }

  @Test
  public void testHashCodeAndEquals() {
    LocalDateInterval subject = new LocalDateInterval(d1, d3);
    LocalDateInterval same = new LocalDateInterval(d1, d3);
    LocalDateInterval i1 = new LocalDateInterval(d1, d2);
    LocalDateInterval i2 = new LocalDateInterval(d2, d3);

    assertEquals(subject, same);
    assertNotEquals(subject, i1);
    assertNotEquals(subject, i2);
    assertEquals(new LocalDateInterval(null, null), LocalDateInterval.unbounded());

    assertEquals(subject.hashCode(), same.hashCode());
    assertNotEquals(subject.hashCode(), i1.hashCode());
    assertNotEquals(subject.hashCode(), i2.hashCode());
    assertEquals(
      new LocalDateInterval(null, null).hashCode(),
      LocalDateInterval.unbounded().hashCode()
    );
  }

  @Test
  public void testToString() {
    assertEquals("[2020-01-07, 2020-01-15]", new LocalDateInterval(d1, d2).toString());
    assertEquals("[MIN, 2020-01-15]", new LocalDateInterval(null, d2).toString());
    assertEquals("[2020-01-07, MAX]", new LocalDateInterval(d1, null).toString());
    assertEquals("[MIN, MAX]", new LocalDateInterval(null, null).toString());
    assertEquals("[MIN, MAX]", LocalDateInterval.unbounded().toString());
  }

  @Test
  public void daysInPeriod() {
    assertEquals(7, new LocalDateInterval(d0, d1).daysInPeriod());
  }
}
