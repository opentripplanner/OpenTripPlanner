package org.opentripplanner.routing.alertpatch;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.time.TimePeriod;

class AlertCalendarTest {

  private static final Instant START = Instant.parse("2024-01-01T10:00:00Z");
  private static final Instant END = Instant.parse("2024-01-01T12:00:00Z");
  private static final Instant LATER_START = Instant.parse("2024-01-02T10:00:00Z");
  private static final Instant LATER_END = Instant.parse("2024-01-02T12:00:00Z");

  @Test
  void neverActive() {
    var subject = AlertCalendar.ofNeverActive();
    assertTrue(subject.isNeverActive());
    assertThat(subject.timePeriods()).isEmpty();
    assertFalse(subject.isActiveDuring(TimePeriod.of(START, END)));
    assertThat(subject.effectiveStart()).isEmpty();
    assertThat(subject.effectiveEnd()).isEmpty();
  }

  @Test
  void ofEmptyCollectionIsNeverActive() {
    assertEquals(AlertCalendar.ofNeverActive(), AlertCalendar.of(List.of()));
  }

  @Test
  void ofAlwaysActive() {
    var subject = AlertCalendar.ofAlwaysActive();
    assertThat(subject.timePeriods()).containsExactly(TimePeriod.ofUnbounded());
    assertFalse(subject.isNeverActive());
    assertTrue(subject.isActiveDuring(TimePeriod.of(START, END)));
    assertThat(subject.effectiveStart()).isEmpty();
    assertThat(subject.effectiveEnd()).isEmpty();
  }

  @Test
  void effectiveBounds() {
    var subject = AlertCalendar.of(
      List.of(TimePeriod.of(LATER_START, LATER_END), TimePeriod.of(START, END))
    );
    assertThat(subject.effectiveStart()).hasValue(START);
    assertThat(subject.effectiveEnd()).hasValue(LATER_END);
  }

  @Test
  void openStartGivesNoEffectiveStart() {
    var subject = AlertCalendar.of(
      List.of(TimePeriod.of(null, END), TimePeriod.of(LATER_START, LATER_END))
    );
    assertThat(subject.effectiveStart()).isEmpty();
    assertThat(subject.effectiveEnd()).hasValue(LATER_END);
  }

  @Test
  void openEndGivesNoEffectiveEnd() {
    var subject = AlertCalendar.of(
      List.of(TimePeriod.of(START, END), TimePeriod.of(LATER_START, null))
    );
    assertThat(subject.effectiveStart()).hasValue(START);
    assertThat(subject.effectiveEnd()).isEmpty();
  }

  @Test
  void isActiveDuringAnyPeriod() {
    var subject = AlertCalendar.of(
      List.of(TimePeriod.of(START, END), TimePeriod.of(LATER_START, LATER_END))
    );
    assertTrue(subject.isActiveDuring(TimePeriod.of(START, END)));
    assertTrue(subject.isActiveDuring(TimePeriod.of(LATER_START, LATER_END)));
    // between the two periods
    assertFalse(subject.isActiveDuring(TimePeriod.of(END, LATER_START.minusSeconds(1))));
    // after both periods
    assertFalse(subject.isActiveDuring(TimePeriod.of(LATER_END, LATER_END.plusSeconds(100))));
  }

  @Test
  void isActiveAt() {
    var subject = AlertCalendar.of(List.of(TimePeriod.of(START, END)));
    // inclusive start
    assertTrue(subject.isActiveAt(START));
    // inside
    assertTrue(subject.isActiveAt(START.plusSeconds(100)));
    // exclusive end
    assertFalse(subject.isActiveAt(END));
    // before
    assertFalse(subject.isActiveAt(START.minusSeconds(1)));
  }
}
