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
    assertEquals(Emission.ofCo2Gram(VALUE + OTHER_VALUE), subject.plus(other));
  }

  @Test
  void multiply() {
    double multiplier = 1.5;
    assertEquals(Emission.ofCo2Gram(VALUE * multiplier), subject.multiply(multiplier));
  }

  @Test
  void dividedBy() {
    double divisor = 1.5;
    assertEquals(Emission.ofCo2Gram(VALUE / divisor), subject.dividedBy(divisor));
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
