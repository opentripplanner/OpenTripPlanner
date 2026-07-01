package org.opentripplanner.core.model.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

public class LocalDateRangeTest {

  private final LocalDate d0 = LocalDate.of(2020, 1, 1);
  private final LocalDate d1 = LocalDate.of(2020, 1, 7);
  private final LocalDate d2 = LocalDate.of(2020, 1, 15);
  private final LocalDate d3 = LocalDate.of(2020, 2, 1);
  private final LocalDate d4 = LocalDate.of(2020, 2, 7);

  @Test
  void unlimitedFromExclusiveFactory() {
    assertTrue(LocalDateRange.ofExclusiveEnd(null, null).isUnbounded());
  }

  @Test
  public void ofInclusiveEndFailsIfEndIsBeforeStart() {
    assertThrows(IllegalArgumentException.class, () ->
      LocalDateRange.ofInclusiveEnd(d1.plusDays(1), d1)
    );
  }

  @Test
  public void ofExclusiveEndFailsIfEndIsBeforeStart() {
    assertThrows(IllegalArgumentException.class, () ->
      LocalDateRange.ofExclusiveEnd(d1.plusDays(1), d1)
    );
  }

  @Test
  public void ofUnboundedContainsAllDates() {
    LocalDateRange u = LocalDateRange.ofUnbounded();
    assertTrue(u.contains(LocalDate.MIN));
    assertTrue(u.contains(LocalDate.MAX.minusDays(1)));
  }

  @Test
  public void isOfUnbounded() {
    assertTrue(LocalDateRange.ofUnbounded().isUnbounded());
    assertFalse(LocalDateRange.ofInclusiveEnd(null, d2).isUnbounded());
    assertFalse(LocalDateRange.ofInclusiveEnd(d2, null).isUnbounded());
  }

  @Test
  public void getStartInclusive() {
    assertEquals(d1, LocalDateRange.ofInclusiveEnd(d1, d2).getStartInclusive());
    assertEquals(LocalDate.MIN, LocalDateRange.ofUnbounded().getStartInclusive());
  }

  @Test
  public void getEndInclusive() {
    assertEquals(d2, LocalDateRange.ofInclusiveEnd(d1, d2).getEndInclusive());
    assertEquals(LocalDate.MAX, LocalDateRange.ofUnbounded().getEndInclusive());
  }

  @Test
  public void getEndExclusive() {
    assertEquals(d2.plusDays(1), LocalDateRange.ofInclusiveEnd(d1, d2).getEndExclusive());
    assertEquals(d2, LocalDateRange.ofExclusiveEnd(d1, d2).getEndExclusive());
    assertEquals(LocalDate.MAX, LocalDateRange.ofUnbounded().getEndExclusive());
  }

  @Test
  public void ofInclusiveEndAndOfExclusiveEndAreEquivalent() {
    assertEquals(
      LocalDateRange.ofInclusiveEnd(d1, d2),
      LocalDateRange.ofExclusiveEnd(d1, d2.plusDays(1))
    );
  }

  @Test
  public void overlap() {
    LocalDateRange subject = LocalDateRange.ofInclusiveEnd(d1, d2);
    LocalDateRange other;

    // First day overlap
    other = LocalDateRange.ofInclusiveEnd(d0, d1);
    assertTrue(subject.overlap(other), subject + " should overlap " + other);

    // Last day overlap
    other = LocalDateRange.ofInclusiveEnd(d2, d3);
    assertTrue(subject.overlap(other), subject + " should overlap " + other);

    // Same periods overlap
    other = LocalDateRange.ofInclusiveEnd(d1, d2);
    assertTrue(subject.overlap(other), subject + " should overlap " + other);

    // Small period overlap part of large
    other = LocalDateRange.ofInclusiveEnd(d0, d4);
    assertTrue(subject.overlap(other), subject + " should overlap " + other);

    // Period ending day before, do NOT overlap
    other = LocalDateRange.ofInclusiveEnd(d0, d1.minusDays(1));
    assertFalse(subject.overlap(other), subject + " should not overlap " + other);

    // Period start day after, do NOT overlap
    other = LocalDateRange.ofInclusiveEnd(d2.plusDays(1), d3);
    assertFalse(subject.overlap(other), subject + " should not overlap " + other);

    // Period overlap with unlimited
    LocalDateRange unlimited = LocalDateRange.ofUnbounded();
    assertTrue(subject.overlap(unlimited), subject + " should overlap unlimited");

    // Unlimited overlap with unlimited
    assertTrue(unlimited.overlap(unlimited), "Unlimited should overlap itself");
  }

  @Test
  public void intersection() {
    LocalDateRange subject = LocalDateRange.ofInclusiveEnd(d1, d2);

    assertEquals(subject, subject.intersection(subject));

    // First day in common
    assertEquals(
      LocalDateRange.ofInclusiveEnd(d1, d1),
      subject.intersection(LocalDateRange.ofInclusiveEnd(d0, d1))
    );

    // Last day in common
    assertEquals(
      LocalDateRange.ofInclusiveEnd(d2, d2),
      subject.intersection(LocalDateRange.ofInclusiveEnd(d2, d3))
    );

    // The intersection of subject and unlimited -> subject
    assertEquals(subject, subject.intersection(LocalDateRange.ofUnbounded()));
  }

  @Test
  public void intersectionFailsIfRangesDoNotOverlap() {
    assertThrows(IllegalArgumentException.class, () ->
      LocalDateRange.ofInclusiveEnd(d0, d1).intersection(
        LocalDateRange.ofInclusiveEnd(d1.plusDays(1), d2)
      )
    );
  }

  @Test
  public void contains() {
    LocalDateRange subject = LocalDateRange.ofInclusiveEnd(d1, d3);
    assertFalse(subject.contains(d0));
    assertTrue(subject.contains(d1));
    assertTrue(subject.contains(d2));
    assertTrue(subject.contains(d3));
    assertFalse(subject.contains(d4));
  }

  @Test
  public void containsWithExclusiveEnd() {
    LocalDateRange subject = LocalDateRange.ofExclusiveEnd(d1, d3);
    assertFalse(subject.contains(d0));
    assertTrue(subject.contains(d1));
    assertTrue(subject.contains(d2));
    assertFalse(subject.contains(d3));
  }

  @Test
  public void testHashCodeAndEquals() {
    LocalDateRange subject = LocalDateRange.ofInclusiveEnd(d1, d3);
    LocalDateRange same = LocalDateRange.ofInclusiveEnd(d1, d3);
    LocalDateRange i1 = LocalDateRange.ofInclusiveEnd(d1, d2);
    LocalDateRange i2 = LocalDateRange.ofInclusiveEnd(d2, d3);

    assertEquals(subject, same);
    assertNotEquals(subject, i1);
    assertNotEquals(subject, i2);
    assertEquals(LocalDateRange.ofInclusiveEnd(null, null), LocalDateRange.ofUnbounded());

    assertEquals(subject.hashCode(), same.hashCode());
    assertNotEquals(subject.hashCode(), i1.hashCode());
    assertNotEquals(subject.hashCode(), i2.hashCode());
    assertEquals(
      LocalDateRange.ofInclusiveEnd(null, null).hashCode(),
      LocalDateRange.ofUnbounded().hashCode()
    );
  }

  @Test
  public void testToString() {
    assertEquals(
      "[2020-01-07, 2020-01-16)",
      LocalDateRange.ofExclusiveEnd(d1, d2.plusDays(1)).toString()
    );
    assertEquals("[MIN, 2020-01-15)", LocalDateRange.ofExclusiveEnd(null, d2).toString());
    assertEquals("[2020-01-07, MAX)", LocalDateRange.ofExclusiveEnd(d1, null).toString());
    assertEquals("[MIN, MAX)", LocalDateRange.ofExclusiveEnd(null, null).toString());
  }

  @Test
  public void daysInPeriod() {
    assertEquals(7, LocalDateRange.ofInclusiveEnd(d0, d1).daysInPeriod());
  }
}
