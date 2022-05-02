package org.opentripplanner.routing.api.request;

public record WheelchairAccessibilityRequest(
  boolean enabled,
  WheelchairAccessibilityFeature trips,
  WheelchairAccessibilityFeature stops,
  float maxSlope
) {
  public static final WheelchairAccessibilityRequest DEFAULT = new WheelchairAccessibilityRequest(
    false,
    WheelchairAccessibilityFeature.ofOnlyAccessible(),
    WheelchairAccessibilityFeature.ofOnlyAccessible(),
    0.0833333333333f // ADA max wheelchair ramp slope is a good default.
  );

  public static WheelchairAccessibilityRequest makeDefault(boolean enabled) {
    return DEFAULT.withEnabled(enabled);
  }

  public WheelchairAccessibilityRequest withEnabled(boolean enabled) {
    return new WheelchairAccessibilityRequest(enabled, this.trips, this.stops, maxSlope);
  }
}
