package org.opentripplanner.standalone.config;

import static org.opentripplanner.routing.api.request.preference.WheelchairPreferences.DEFAULT;

import org.opentripplanner.routing.api.request.preference.AccessibilityPreferences;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;

public class WheelchairAccessibilityRequestMapper {

  static WheelchairPreferences mapAccessibilityRequest(NodeAdapter a) {
    return new WheelchairPreferences(
      mapAccessibilityFeature(a.path("trip"), DEFAULT.trip()),
      mapAccessibilityFeature(a.path("stop"), DEFAULT.stop()),
      mapAccessibilityFeature(a.path("elevator"), DEFAULT.elevator()),
      a.asDouble("inaccessibleStreetReluctance", DEFAULT.inaccessibleStreetReluctance()),
      a.asDouble("maxSlope", DEFAULT.maxSlope()),
      a.asDouble("slopeExceededReluctance", DEFAULT.slopeExceededReluctance()),
      a.asDouble("stairsReluctance", DEFAULT.stairsReluctance())
    );
  }

  private static AccessibilityPreferences mapAccessibilityFeature(
    NodeAdapter adapter,
    AccessibilityPreferences defaultValue
  ) {
    var onlyAccessible = adapter.asBoolean(
      "onlyConsiderAccessible",
      defaultValue.onlyConsiderAccessible()
    );

    var unknownCost = adapter.asInt("unknownCost", 60 * 10);
    var inaccessibleCost = adapter.asInt("inaccessibleCost", 60 * 60);
    if (onlyAccessible) {
      return AccessibilityPreferences.ofOnlyAccessible();
    } else {
      return AccessibilityPreferences.ofCost(unknownCost, inaccessibleCost);
    }
  }
}
