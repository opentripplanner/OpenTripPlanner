package org.opentripplanner.routing.api.request.refactor.preference;

import org.opentripplanner.routing.api.request.WheelchairAccessibilityRequest;

public class WheelchairPreferences {
  WheelchairAccessibilityRequest accessibility = WheelchairAccessibilityRequest.DEFAULT;
  double maxSlope = 0.0833333333333;
  double slopeTooSteepCostFactor = 10.0;
}
