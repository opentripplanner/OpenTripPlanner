package org.opentripplanner.framework.model;

import static java.lang.Double.compare;

import java.io.Serializable;

/**
 * A representation of the weight of something in grams.
 */
public final class Gram implements Serializable, Comparable<Gram> {

  private final double value;

  public Gram(double value) {
    this.value = value;
  }

  public Gram plus(Gram g) {
    return new Gram(this.value + g.value);
  }

  public Gram multiply(int factor) {
    return new Gram(this.value * factor);
  }

  public Gram multiply(double factor) {
    return new Gram(this.value * factor);
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
}
