package org.opentripplanner.core.model.time;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class TimePeriodTest {

  private static final Instant START = Instant.parse("2024-01-01T10:00:00Z");
  private static final Instant END = Instant.parse("2024-01-01T12:00:00Z");

  @Test
  void of() {
    var subject = TimePeriod.of(START, END);
    assertThat(subject.start()).hasValue(START);
    assertThat(subject.end()).hasValue(END);
    assertFalse(subject.hasUnboundedStart());
    assertFalse(subject.hasUnboundedEnd());
  }

  @Test
  void ofUnbounded() {
    var subject = TimePeriod.ofUnbounded();
    assertThat(subject.start()).isEmpty();
    assertThat(subject.end()).isEmpty();
    assertTrue(subject.hasUnboundedStart());
    assertTrue(subject.hasUnboundedEnd());
  }

  @Test
  void unboundedStart() {
    var subject = TimePeriod.of(null, END);
    assertTrue(subject.hasUnboundedStart());
    assertFalse(subject.hasUnboundedEnd());
  }

  @Test
  void unboundedEnd() {
    var subject = TimePeriod.of(START, null);
    assertFalse(subject.hasUnboundedStart());
    assertTrue(subject.hasUnboundedEnd());
  }

  @Test
  void startAfterEndIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> TimePeriod.of(END, START));
  }

  @Test
  void overlapsBoundedPeriod() {
    var subject = TimePeriod.of(START, END);

    // entirely before
    assertFalse(subject.overlaps(TimePeriod.of(START.minusSeconds(200), START.minusSeconds(100))));
    // ends exactly at the start - the start is inclusive, but end is exclusive
    assertFalse(subject.overlaps(TimePeriod.of(START.minusSeconds(100), START)));
    // overlapping the start
    assertTrue(subject.overlaps(TimePeriod.of(START.minusSeconds(100), START.plusSeconds(1))));
    // inside
    assertTrue(subject.overlaps(TimePeriod.of(START.plusSeconds(100), END.minusSeconds(100))));
    // overlapping the end
    assertTrue(subject.overlaps(TimePeriod.of(END.minusSeconds(100), END.plusSeconds(100))));
    // starts exactly at the end - the end is exclusive
    assertFalse(subject.overlaps(TimePeriod.of(END, END.plusSeconds(100))));
    // entirely after
    assertFalse(subject.overlaps(TimePeriod.of(END.plusSeconds(100), END.plusSeconds(200))));
  }

  @Test
  void overlapsUnboundedStart() {
    var subject = TimePeriod.of(null, END);
    assertTrue(subject.overlaps(TimePeriod.of(START.minusSeconds(2000), START.minusSeconds(1000))));
    assertFalse(subject.overlaps(TimePeriod.of(END.plusSeconds(100), END.plusSeconds(200))));
  }

  @Test
  void overlapsUnboundedEnd() {
    var subject = TimePeriod.of(START, null);
    assertFalse(subject.overlaps(TimePeriod.of(START.minusSeconds(200), START.minusSeconds(100))));
    assertTrue(subject.overlaps(TimePeriod.of(END.plusSeconds(100), END.plusSeconds(200))));
  }

  @Test
  void overlapsUnboundedBoundsOnOtherPeriod() {
    var subject = TimePeriod.of(START, END);
    // other period has an unbounded end, starting before this period ends
    assertTrue(subject.overlaps(TimePeriod.of(END.minusSeconds(100), null)));
    // other period has an unbounded end, starting exactly at this period's end - exclusive
    assertFalse(subject.overlaps(TimePeriod.of(END, null)));
    // other period has an unbounded start, ending after this period starts
    assertTrue(subject.overlaps(TimePeriod.of(null, START.plusSeconds(1))));
    // other period has an unbounded start, ending exactly at this period's start. Because end
    // is exclusive, there is no overlap
    assertFalse(subject.overlaps(TimePeriod.of(null, START)));
    // other period is entirely unbounded
    assertTrue(subject.overlaps(TimePeriod.ofUnbounded()));
  }

  @Test
  void unboundedOverlapsEverything() {
    var subject = TimePeriod.ofUnbounded();
    assertTrue(subject.overlaps(TimePeriod.of(START, END)));
    assertTrue(subject.overlaps(TimePeriod.of(Instant.EPOCH, Instant.EPOCH)));
    assertTrue(subject.overlaps(TimePeriod.ofUnbounded()));
  }

  @Test
  void containsBoundedPeriod() {
    var subject = TimePeriod.of(START, END);

    // before the start
    assertFalse(subject.contains(START.minusSeconds(1)));
    // exactly at the start - inclusive
    assertTrue(subject.contains(START));
    // inside
    assertTrue(subject.contains(START.plusSeconds(100)));
    // exactly at the end - exclusive
    assertFalse(subject.contains(END));
    // after the end
    assertFalse(subject.contains(END.plusSeconds(1)));
  }

  @Test
  void containsUnboundedStart() {
    var subject = TimePeriod.of(null, END);

    // far in the past is contained because the start is unbounded
    assertTrue(subject.contains(Instant.EPOCH));
    assertTrue(subject.contains(START));
    // exactly at the end - exclusive
    assertFalse(subject.contains(END));
    assertFalse(subject.contains(END.plusSeconds(1)));
  }

  @Test
  void containsUnboundedEnd() {
    var subject = TimePeriod.of(START, null);

    // before the start
    assertFalse(subject.contains(START.minusSeconds(1)));
    // exactly at the start - inclusive
    assertTrue(subject.contains(START));
    // far in the future is contained because the end is unbounded
    assertTrue(subject.contains(END.plusSeconds(1_000_000)));
  }

  @Test
  void unboundedContainsEverything() {
    var subject = TimePeriod.ofUnbounded();
    assertTrue(subject.contains(Instant.EPOCH));
    assertTrue(subject.contains(START));
    assertTrue(subject.contains(END.plusSeconds(1_000_000)));
  }
}
