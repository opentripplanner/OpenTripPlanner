package org.opentripplanner.routing.api.request;

/**
 * @param slopeTooSteepPenalty What penalty factor should be given to street edges, which are over
 *                             the max slope. Set to negative for disable routing on too steep
 *                             edges.
 */
public record WheelchairAccessibilityRequest(
  boolean enabled,
  WheelchairAccessibilityFeature trips,
  WheelchairAccessibilityFeature stops,
  float maxSlope,
  float slopeTooSteepPenalty
) {
  public static final WheelchairAccessibilityRequest DEFAULT = new WheelchairAccessibilityRequest(
    false,
    WheelchairAccessibilityFeature.ofOnlyAccessible(),
    WheelchairAccessibilityFeature.ofOnlyAccessible(),
    0.0833333333333f, // ADA max wheelchair ramp slope is a good default.
    10
  );

  public static WheelchairAccessibilityRequest makeDefault(boolean enabled) {
    return DEFAULT.withEnabled(enabled);
  }

  public WheelchairAccessibilityRequest withEnabled(boolean enabled) {
    return new WheelchairAccessibilityRequest(
      enabled,
      this.trips,
      this.stops,
      maxSlope,
      slopeTooSteepPenalty
    );
  }
}
