package org.opentripplanner.ext.flex.trip;

import java.io.Serializable;
import java.time.Duration;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.framework.tostring.ToStringBuilder;

public class DurationModifier implements Serializable {

  public static DurationModifier NONE = new DurationModifier(Duration.ZERO, 1);
  private final int offset;
  private final float factor;

  public DurationModifier(Duration offset, float factor) {
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

  /**
   * Check if this instance actually modifies the duration or simply passes it back without
   * change.
   */
  boolean modifies() {
    return offset != 0 && factor != 1.0;
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(DurationModifier.class)
      .addNum("factor", factor)
      .addDurationSec("offset", offset)
      .toString();
  }
}
