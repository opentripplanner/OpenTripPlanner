package org.opentripplanner.routing.api.request.refactor.preference;

import java.io.Serializable;
import javax.annotation.Nonnull;
import org.opentripplanner.routing.api.request.WheelchairAccessibilityRequest;

public class WheelchairPreferences implements Cloneable, Serializable {

  /**
   * Whether the trip must be wheelchair-accessible and how strictly this should be interpreted.
   */
  @Nonnull
  private WheelchairAccessibilityRequest accessibility = WheelchairAccessibilityRequest.DEFAULT;

  // TODO: 2022-08-18 Is it new? never used
  private double maxSlope = 0.0833333333333;

  // TODO: 2022-08-25 this was in specification but we never use it
  // TODO: 2022-08-25 do we need it?
  private double slopeTooSteepCostFactor = 10.0;

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
