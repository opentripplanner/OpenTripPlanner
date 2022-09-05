package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;
import javax.annotation.Nonnull;

// TODO VIA: Remove this class and use existing WheelchairAccessibilityRequest instead
public class WheelchairPreferences implements Cloneable, Serializable {

  /**
   * Whether the trip must be wheelchair-accessible and how strictly this should be interpreted.
   */
  @Nonnull
  private WheelchairAccessibilityRequest accessibility = WheelchairAccessibilityRequest.DEFAULT;

  // TODO VIA: 2022-08-18 Is it new? never used
  private double maxSlope = 0.0833333333333;

  // TODO VIA: 2022-08-25 this was in specification but we never use it
  // TODO VIA: 2022-08-25 do we need it?
  private double slopeTooSteepCostFactor = 10.0;

  public WheelchairPreferences clone() {
    try {
      // TODO VIA: 2022-08-26 Skipping WheelchairAccessibilityRequest (that's how it was before)

      var clone = (WheelchairPreferences) super.clone();

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }

  public void setAccessible(boolean wheelchair) {
    this.accessibility = this.accessibility.withEnabled(wheelchair);
  }

  public void setAccessibility(@Nonnull WheelchairAccessibilityRequest accessibility) {
    this.accessibility = accessibility;
  }

  @Nonnull
  public WheelchairAccessibilityRequest accessibility() {
    return accessibility;
  }

  public void setMaxSlope(double maxSlope) {
    this.maxSlope = maxSlope;
  }

  public double maxSlope() {
    return maxSlope;
  }
}
