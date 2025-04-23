package org.opentripplanner.model.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.model.Gram;

class EmissionTest {

  public static final double VALUE = 12.0;
  public static final double OTHER_VALUE = 7.5;
  private static final Gram CO2_VALUE = Gram.of(VALUE);
  private static final Gram OTHER_CO2_VALUE = Gram.of(OTHER_VALUE);

  private final Emission subject = Emission.of(CO2_VALUE);
  private final Emission other = Emission.of(OTHER_CO2_VALUE);

  @Test
  void co2() {
    assertEquals(CO2_VALUE, subject.co2());
  }

  @Test
  void plus() {
    assertEquals(Emission.co2_g(VALUE + OTHER_VALUE), subject.plus(other));
  }

  @Test
  void multiply() {
    double scalar = 1.5;
    assertEquals(Emission.co2_g(VALUE * scalar), subject.multiply(scalar));
  }

  @Test
  void dividedBy() {
    double scalar = 1.5;
    assertEquals(Emission.co2_g(VALUE / scalar), subject.dividedBy(scalar));
  }

  @Test
  void isZero() {
    assertTrue(Emission.ZERO.isZero());
    assertTrue(Emission.of(Gram.ZERO).isZero());
    assertFalse(subject.isZero());
  }

  @Test
  void testEqualsAndHashCode() {
    var same = Emission.of(CO2_VALUE);

    assertEquals(same, subject);
    assertNotEquals(other, subject);

    assertEquals(same.hashCode(), subject.hashCode());
    assertNotEquals(other.hashCode(), subject.hashCode());
  }

  @Test
  void testToString() {
    assertEquals("Emission{CO₂: 12g}", subject.toString());
  }
}
