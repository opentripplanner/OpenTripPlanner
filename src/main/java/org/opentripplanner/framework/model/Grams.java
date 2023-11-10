package org.opentripplanner.framework.model;

import static java.lang.Double.compare;

import java.io.Serializable;

/**
 * A representation of the weight of something in grams.
 */
public final class Grams implements Serializable, Comparable<Grams> {

  private final double value;

  public Grams(double value) {
    this.value = value;
  }

  public Grams plus(Grams g) {
    return new Grams(this.value + g.value);
  }

  public Grams multiply(int factor) {
    return new Grams(this.value * factor);
  }

  public Grams multiply(double factor) {
    return new Grams(this.value * factor);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    var that = (Grams) o;
    return value == that.value;
  }

  @Override
  public int compareTo(Grams o) {
    return compare(value, o.value);
  }

  @Override
  public String toString() {
    return this.value + "g";
  }

  public double asDouble() {
    return this.value;
  }
}
