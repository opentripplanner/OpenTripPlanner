package org.opentripplanner.routing.api.request.refactor.preference;

import javax.annotation.Nonnull;
import org.opentripplanner.routing.api.request.WheelchairAccessibilityRequest;

public class WheelchairPreferences {

  /**
   * Whether the trip must be wheelchair-accessible and how strictly this should be interpreted.
   */
  @Nonnull
  private WheelchairAccessibilityRequest accessibility = WheelchairAccessibilityRequest.DEFAULT;

  // TODO: 2022-08-18 Is it new?
  private double maxSlope = 0.0833333333333;
  // TODO: 2022-08-18 Is it new?
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

  public void setSlopeTooSteepCostFactor(double slopeTooSteepCostFactor) {
    this.slopeTooSteepCostFactor = slopeTooSteepCostFactor;
  }

  public double slopeTooSteepCostFactor() {
    return slopeTooSteepCostFactor;
  }
}
