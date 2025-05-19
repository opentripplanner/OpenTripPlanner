package org.opentripplanner.framework.model;

import static java.lang.Double.compare;

import java.io.Serializable;

/**
 * A representation of the weight of something in grams.
 */
public final class Gram implements Serializable, Comparable<Gram> {

  public static final Gram ZERO = new Gram(0.0);

  private final double value;

  private Gram(double value) {
    this.value = value;
  }

  public static Gram of(double value) {
    return value == 0.0 ? ZERO : new Gram(value);
  }

  public static Gram ofNullable(Double value) {
    return value == null ? ZERO : Gram.of(value);
  }

  public Gram plus(Gram g) {
    return of(this.value + g.value);
  }

  public Gram multiply(double factor) {
    return of(this.value * factor);
  }

  public Gram dividedBy(double scalar) {
    return new Gram(this.value / scalar);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    var that = (Gram) o;
    return value == that.value;
  }

  @Override
  public int hashCode() {
    return Double.hashCode(value);
  }

  @Override
  public int compareTo(Gram o) {
    return compare(value, o.value);
  }

  @Override
  public String toString() {
    return this.value + "g";
  }

  public double asDouble() {
    return this.value;
  }

  public boolean isZero() {
    return value == 0.0;
  }
}
