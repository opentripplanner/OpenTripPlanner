package org.opentripplanner.transit.model.basic;

import java.util.Objects;

public class Ratio {

  private final Double ratio;

  public Ratio(Double ratio) {
    if (ratio < 0d || ratio > 1d) {
      throw new IllegalArgumentException("Ratio must be in range [0,1]");
    }

    this.ratio = ratio;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof Ratio ratio) {
      return ratio.ratio == this.ratio;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.ratio);
  }

  @Override
  public String toString() {
    return this.ratio.toString();
  }

  public Double asDouble() {
    return ratio;
  }
}
