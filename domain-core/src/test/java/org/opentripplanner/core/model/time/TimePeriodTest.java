package org.opentripplanner.core.model.time;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    assertFalse(subject.hasOpenStart());
    assertFalse(subject.hasOpenEnd());
  }

  @Test
  void ofUnbounded() {
    var subject = TimePeriod.ofUnbounded();
    assertThat(subject.start()).isEmpty();
    assertThat(subject.end()).isEmpty();
    assertTrue(subject.hasOpenStart());
    assertTrue(subject.hasOpenEnd());
  }

  @Test
  void openStart() {
    var subject = TimePeriod.of(null, END);
    assertTrue(subject.hasOpenStart());
    assertFalse(subject.hasOpenEnd());
  }

  @Test
  void openEnd() {
    var subject = TimePeriod.of(START, null);
    assertFalse(subject.hasOpenStart());
    assertTrue(subject.hasOpenEnd());
  }

  @Test
  void startAfterEndIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> TimePeriod.of(END, START));
  }

  @Test
  void overlapsBoundedPeriod() {
    var subject = TimePeriod.of(START, END);

    // entirely before
    assertFalse(subject.overlaps(START.minusSeconds(200), START.minusSeconds(100)));
    // ends exactly at the start - the start is inclusive
    assertTrue(subject.overlaps(START.minusSeconds(100), START));
    // overlapping the start
    assertTrue(subject.overlaps(START.minusSeconds(100), START.plusSeconds(100)));
    // inside
    assertTrue(subject.overlaps(START.plusSeconds(100), END.minusSeconds(100)));
    // overlapping the end
    assertTrue(subject.overlaps(END.minusSeconds(100), END.plusSeconds(100)));
    // starts exactly at the end - the end is exclusive
    assertFalse(subject.overlaps(END, END.plusSeconds(100)));
    // entirely after
    assertFalse(subject.overlaps(END.plusSeconds(100), END.plusSeconds(200)));
  }

  @Test
  void overlapsOpenStart() {
    var subject = TimePeriod.of(null, END);
    assertTrue(subject.overlaps(START.minusSeconds(2000), START.minusSeconds(1000)));
    assertFalse(subject.overlaps(END.plusSeconds(100), END.plusSeconds(200)));
  }

  @Test
  void overlapsOpenEnd() {
    var subject = TimePeriod.of(START, null);
    assertFalse(subject.overlaps(START.minusSeconds(200), START.minusSeconds(100)));
    assertTrue(subject.overlaps(END.plusSeconds(100), END.plusSeconds(200)));
  }

  @Test
  void unboundedOverlapsEverything() {
    var subject = TimePeriod.ofUnbounded();
    assertTrue(subject.overlaps(START, END));
    assertTrue(subject.overlaps(Instant.EPOCH, Instant.EPOCH));
  }
}
