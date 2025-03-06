package org.opentripplanner.routing.api.request.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class TimeAndCostPenaltyForEnumTest {

  private static final double COST_FACTOR = 1.5;
  private static final TimeAndCostPenalty PERCH_PENALTY = TimeAndCostPenalty.of(
    "11s + 1.2t",
    COST_FACTOR
  );
  private final TimeAndCostPenaltyForEnum<Fish> subject = TimeAndCostPenaltyForEnum.of(Fish.class)
    .with(Fish.PERCH, PERCH_PENALTY)
    .build();

  @Test
  void ofDefault() {
    var dft = TimeAndCostPenaltyForEnum.ofDefault(Fish.class);
    assertEquals(TimeAndCostPenalty.ZERO, dft.valueOf(Fish.PERCH));
  }

  @Test
  void copyOf() {
    assertSame(subject, subject.copyOf().build());
  }

  @Test
  void valueOf() {
    assertEquals(PERCH_PENALTY, subject.valueOf(Fish.PERCH));
    assertEquals(TimeAndCostPenalty.ZERO, subject.valueOf(Fish.COD));
  }

  @Test
  void isSet() {
    assertTrue(subject.isSet(Fish.PERCH));
    assertFalse(subject.isSet(Fish.COD));
  }

  @Test
  void testToString() {
    assertEquals(
      "TimeAndCostPenaltyForEnum{PERCH: (timePenalty: 11s + 1.20 t, costFactor: 1.50)}",
      subject.toString()
    );
    // Test toSting for builder
    assertEquals(
      "Builder{PERCH: (timePenalty: 11s + 1.20 t, costFactor: 1.50)}",
      subject.copyOf().toString()
    );
  }

  @Test
  void testEqualsAndHashCode() {
    var same = TimeAndCostPenaltyForEnum.of(Fish.class).with(Fish.PERCH, PERCH_PENALTY).build();
    var other = TimeAndCostPenaltyForEnum.of(Fish.class)
      .with(Fish.COD, new TimeAndCostPenalty(TimePenalty.of("1s + 1.0  t"), 1.5))
      .build();

    // equals(...)
    assertEquals(same, subject);
    assertNotEquals(other, subject);

    // hashCode()
    assertEquals(same.hashCode(), subject.hashCode());
    assertNotEquals(other.hashCode(), subject.hashCode());
  }

  @Test
  void testOf() {
    var c = TimeAndCostPenaltyForEnum.of(Fish.class)
      .with(Fish.COD, TimeAndCostPenalty.of("1s + 3t", 0.2))
      .build();
    assertEquals(
      "TimeAndCostPenaltyForEnum{COD: (timePenalty: 1s + 3.0 t, costFactor: 0.20)}",
      c.toString()
    );
  }

  @Test
  void testBuilderWithRemoveExistingValue() {
    assertEquals(
      "TimeAndCostPenaltyForEnum{}",
      subject.copyOf().with(Fish.PERCH, TimeAndCostPenalty.ZERO).build().toString()
    );
  }

  @Test
  void testBuilderWithMap() {
    var c = TimeAndCostPenaltyForEnum.of(Fish.class)
      .withValues(Map.of(Fish.PERCH, TimeAndCostPenalty.of("2s + 4t", 2.0)))
      .build();

    assertEquals(
      "TimeAndCostPenaltyForEnum{PERCH: (timePenalty: 2s + 4.0 t, costFactor: 2.0)}",
      c.toString()
    );
  }

  @Test
  void testBuildApply() {
    var c = TimeAndCostPenaltyForEnum.of(Fish.class)
      .apply(b -> b.with(Fish.PERCH, TimeAndCostPenalty.of("3s + 5t", 0.1)))
      .build();

    assertEquals(
      "TimeAndCostPenaltyForEnum{PERCH: (timePenalty: 3s + 5.0 t, costFactor: 0.10)}",
      c.toString()
    );
  }

  enum Fish {
    COD,
    PERCH,
  }
}
