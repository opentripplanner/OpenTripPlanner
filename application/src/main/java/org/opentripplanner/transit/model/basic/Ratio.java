package org.opentripplanner.transit.model.basic;

import java.util.Objects;

public class Ratio {
  public final Double ratio;

  public Ratio(Double ratio) throws IllegalArgumentException {
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
    return Objects.hash(this.ratio, "Ratio");
  }

  @Override
  public String toString() {
    return this.ratio.toString();
  }
}
