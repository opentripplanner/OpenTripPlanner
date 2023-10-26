package org.opentripplanner.framework.model;

import static java.lang.Double.compare;

import java.io.Serializable;

public final class Grams implements Serializable, Comparable<Grams> {

  private final double value;

  public Grams(double value) {
    this.value = value;
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
