package org.opentripplanner.ext.flex.trip;

import java.io.Serializable;
import java.time.Duration;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.framework.tostring.ToStringBuilder;

public class FlexDurationFactors implements Serializable {

  public static FlexDurationFactors ZERO = new FlexDurationFactors(Duration.ZERO, 1);
  private final int offset;
  private final float factor;

  public FlexDurationFactors(Duration offset, float factor) {
    if (factor < 0.1) {
      throw new IllegalArgumentException("Flex duration factor must not be less than 0.1");
    }
    this.offset = (int) DurationUtils.requireNonNegative(offset).toSeconds();
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

  @Override
  public String toString() {
    return ToStringBuilder
      .of(FlexDurationFactors.class)
      .addNum("factor", factor)
      .addDurationSec("offset", offset)
      .toString();
  }
}
