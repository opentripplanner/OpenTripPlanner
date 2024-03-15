package org.opentripplanner.ext.flex.trip;

import java.time.Duration;

public class FlexDurationFactors {

  public static FlexDurationFactors ZERO = new FlexDurationFactors(Duration.ZERO, 1);
  private final int offset;
  private final float factor;

  public FlexDurationFactors(Duration offset, float factor) {
    this.offset = (int) offset.toSeconds();
    this.factor = factor;
  }

  public float factor() {
    return factor;
  }

  public int offsetInSeconds() {
    return offset;
  }

  boolean nonZero() {
    return offset != 0 && factor != 1.0;
  }
}
