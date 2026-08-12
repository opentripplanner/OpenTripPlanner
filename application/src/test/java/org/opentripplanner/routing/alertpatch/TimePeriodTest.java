package org.opentripplanner.routing.alertpatch;

import static com.google.common.truth.Truth.assertThat;
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
    assertThat(subject.hasOpenStart()).isFalse();
    assertThat(subject.hasOpenEnd()).isFalse();
  }

  @Test
  void ofUnbounded() {
    var subject = TimePeriod.ofUnbounded();
    assertThat(subject.start()).isEmpty();
    assertThat(subject.end()).isEmpty();
    assertThat(subject.hasOpenStart()).isTrue();
    assertThat(subject.hasOpenEnd()).isTrue();
  }

  @Test
  void openStart() {
    var subject = TimePeriod.of(null, END);
    assertThat(subject.hasOpenStart()).isTrue();
    assertThat(subject.hasOpenEnd()).isFalse();
  }

  @Test
  void openEnd() {
    var subject = TimePeriod.of(START, null);
    assertThat(subject.hasOpenStart()).isFalse();
    assertThat(subject.hasOpenEnd()).isTrue();
  }

  @Test
  void startAfterEndIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> TimePeriod.of(END, START));
  }

  @Test
  void overlapsBoundedPeriod() {
    var subject = TimePeriod.of(START, END);

    // entirely before
    assertThat(subject.overlaps(START.minusSeconds(200), START.minusSeconds(100))).isFalse();
    // ends exactly at the start - the start is inclusive
    assertThat(subject.overlaps(START.minusSeconds(100), START)).isTrue();
    // overlapping the start
    assertThat(subject.overlaps(START.minusSeconds(100), START.plusSeconds(100))).isTrue();
    // inside
    assertThat(subject.overlaps(START.plusSeconds(100), END.minusSeconds(100))).isTrue();
    // overlapping the end
    assertThat(subject.overlaps(END.minusSeconds(100), END.plusSeconds(100))).isTrue();
    // starts exactly at the end - the end is exclusive
    assertThat(subject.overlaps(END, END.plusSeconds(100))).isFalse();
    // entirely after
    assertThat(subject.overlaps(END.plusSeconds(100), END.plusSeconds(200))).isFalse();
  }

  @Test
  void overlapsOpenStart() {
    var subject = TimePeriod.of(null, END);
    assertThat(subject.overlaps(START.minusSeconds(2000), START.minusSeconds(1000))).isTrue();
    assertThat(subject.overlaps(END.plusSeconds(100), END.plusSeconds(200))).isFalse();
  }

  @Test
  void overlapsOpenEnd() {
    var subject = TimePeriod.of(START, null);
    assertThat(subject.overlaps(START.minusSeconds(200), START.minusSeconds(100))).isFalse();
    assertThat(subject.overlaps(END.plusSeconds(100), END.plusSeconds(200))).isTrue();
  }

  @Test
  void unboundedOverlapsEverything() {
    var subject = TimePeriod.ofUnbounded();
    assertThat(subject.overlaps(START, END)).isTrue();
    assertThat(subject.overlaps(Instant.EPOCH, Instant.EPOCH)).isTrue();
  }
}
