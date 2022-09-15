package org.opentripplanner.standalone.config;

import static org.opentripplanner.routing.api.request.preference.WheelchairAccessibilityPreferences.DEFAULT;

import org.opentripplanner.routing.api.request.preference.WheelchairAccessibilityFeature;
import org.opentripplanner.routing.api.request.preference.WheelchairAccessibilityPreferences;

public class WheelchairAccessibilityRequestMapper {

  static WheelchairAccessibilityPreferences mapAccessibilityRequest(NodeAdapter a) {
    var trips = mapAccessibilityFeature(a.path("trip"), DEFAULT.trip());
    var stops = mapAccessibilityFeature(a.path("stop"), DEFAULT.stop());
    var elevators = mapAccessibilityFeature(a.path("elevator"), DEFAULT.elevator());
    var inaccessibleStreetReluctance = (float) a.asDouble(
      "inaccessibleStreetReluctance",
      DEFAULT.inaccessibleStreetReluctance()
    );
    var maxSlope = a.asDouble("maxSlope", DEFAULT.maxSlope());
    var slopeExceededReluctance = a.asDouble(
      "slopeExceededReluctance",
      DEFAULT.slopeExceededReluctance()
    );
    var stairsReluctance = a.asDouble("stairsReluctance", DEFAULT.stairsReluctance());

    return new WheelchairAccessibilityPreferences(
      trips,
      stops,
      elevators,
      inaccessibleStreetReluctance,
      maxSlope,
      slopeExceededReluctance,
      stairsReluctance
    );
  }

  private static WheelchairAccessibilityFeature mapAccessibilityFeature(
    NodeAdapter adapter,
    WheelchairAccessibilityFeature deflt
  ) {
    var onlyAccessible = adapter.asBoolean(
      "onlyConsiderAccessible",
      deflt.onlyConsiderAccessible()
    );

    var unknownCost = adapter.asInt("unknownCost", 60 * 10);
    var inaccessibleCost = adapter.asInt("inaccessibleCost", 60 * 60);
    if (onlyAccessible) {
      return WheelchairAccessibilityFeature.ofOnlyAccessible();
    } else {
      return WheelchairAccessibilityFeature.ofCost(unknownCost, inaccessibleCost);
    }
  }
}
