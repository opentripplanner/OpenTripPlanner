package org.opentripplanner.model.plan;

import java.io.Serializable;
import java.util.Objects;
import org.opentripplanner.framework.model.Gram;
import org.opentripplanner.utils.lang.Sandbox;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Represents the emission of a journey. Each type of emissions has its own field and unit.
 */
@Sandbox
public final class Emission implements Serializable {

  public static final Emission ZERO = new Emission(Gram.ZERO);

  private final Gram co2;

  private Emission(Gram co2) {
    this.co2 = Objects.requireNonNull(co2);
  }

  public static Emission of(Gram co2) {
    return new Emission(co2);
  }

  /** Convinience factory method for creating test data */
  public static Emission ofCo2Gram(double co2_g) {
    return new Emission(Gram.of(co2_g));
  }

  public Gram co2() {
    return co2;
  }

  public Emission plus(Emission right) {
    return new Emission(co2.plus(right.co2));
  }

  /**
   * Multiply all emissions with the given {@code multiplier} and return the new product.
   */
  public Emission multiply(double multiplier) {
    return new Emission(co2.multiply(multiplier));
  }

  /**
   * Devide all emissions by the given {@code divisor} and return the new result.
   */
  public Emission dividedBy(double divisor) {
    return new Emission(co2.dividedBy(divisor));
  }

  public boolean isZero() {
    return co2.isZero();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    return co2.equals(((Emission) o).co2);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(co2);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(Emission.class).addObj("CO₂", co2, Gram.ZERO).toString();
  }
}
