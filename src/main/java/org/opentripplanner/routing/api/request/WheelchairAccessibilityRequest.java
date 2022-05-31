package org.opentripplanner.routing.api.request;

/**
 * @param slopeExceededReluctance What factor should be given to street edges, which are over the
 *                                max slope. The penalty is not static but scales with how much you
 *                                exceed the maximum slope. Set to negative to disable routing on
 *                                too steep edges.
 * @param stairsReluctance        Stairs are not completely excluded for wheelchair users but
 *                                severely punished. This value determines how much they are
 *                                punished. This should be a very high value as you want to only
 *                                include stairs as a last result.
 */
public record WheelchairAccessibilityRequest(
  boolean enabled,
  WheelchairAccessibilityFeature trip,
  WheelchairAccessibilityFeature stop,
  WheelchairAccessibilityFeature elevator,
  double inaccessibleStreetReluctance,
  double maxSlope,
  double slopeExceededReluctance,
  double stairsReluctance
) {
  public static final WheelchairAccessibilityRequest DEFAULT = new WheelchairAccessibilityRequest(
    false,
    WheelchairAccessibilityFeature.ofOnlyAccessible(),
    WheelchairAccessibilityFeature.ofOnlyAccessible(),
    // it's very common for elevators in OSM to have unknown wheelchair accessibility since they are assumed to be so
    // for that reason they only have a small default penalty for unknown accessibility
    WheelchairAccessibilityFeature.ofCost(20, 3600),
    // since most streets have no accessibility information, we don't add a cost for that
    25,
    0.0833333333333f, // ADA max wheelchair ramp slope is a good default.
    1.1f,
    100
  );

  public static WheelchairAccessibilityRequest makeDefault(boolean enabled) {
    return DEFAULT.withEnabled(enabled);
  }

  public WheelchairAccessibilityRequest withEnabled(boolean enabled) {
    return new WheelchairAccessibilityRequest(
      enabled,
      trip,
      stop,
      elevator,
      inaccessibleStreetReluctance,
      maxSlope,
      slopeExceededReluctance,
      stairsReluctance
    );
  }
}
