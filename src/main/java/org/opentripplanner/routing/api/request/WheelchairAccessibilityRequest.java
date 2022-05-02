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
  WheelchairAccessibilityFeature elevators,
  WheelchairAccessibilityFeature streets,
  float maxSlope,
  float slopeTooSteepPenalty,
  float stairsReluctance
) {
  public static final WheelchairAccessibilityRequest DEFAULT = new WheelchairAccessibilityRequest(
    false,
    WheelchairAccessibilityFeature.ofOnlyAccessible(),
    WheelchairAccessibilityFeature.ofOnlyAccessible(),
    // it's very common for elevators in OSM to have unknown wheelchair accessibility since they are assumed to be so
    // for that reason they only have a small default penalty for unknown accessibility
    WheelchairAccessibilityFeature.ofCost(30, 3600),
    // since most streets have no accessibility information, we don't add a cost for that
    WheelchairAccessibilityFeature.ofCost(0, 3600),
    0.0833333333333f, // ADA max wheelchair ramp slope is a good default.
    10,
    25
  );

  public static WheelchairAccessibilityRequest makeDefault(boolean enabled) {
    return DEFAULT.withEnabled(enabled);
  }

  public WheelchairAccessibilityRequest withEnabled(boolean enabled) {
    return new WheelchairAccessibilityRequest(
      enabled,
      trips,
      stops,
      elevators,
      streets,
      maxSlope,
      slopeTooSteepPenalty,
      stairsReluctance
    );
  }
}
