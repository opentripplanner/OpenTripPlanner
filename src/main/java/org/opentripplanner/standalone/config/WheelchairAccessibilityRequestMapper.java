package org.opentripplanner.standalone.config;

import static org.opentripplanner.routing.api.request.WheelchairAccessibilityRequest.DEFAULTS;

import org.opentripplanner.routing.api.request.WheelchairAccessibilityFeature;
import org.opentripplanner.routing.api.request.WheelchairAccessibilityRequest;

public class WheelchairAccessibilityRequestMapper {

  static WheelchairAccessibilityRequest mapAccessibilityRequest(NodeAdapter a) {
    var trips = mapAccessibilityFeature(a.path("trips"), DEFAULTS.trips());
    var stops = mapAccessibilityFeature(a.path("stops"), DEFAULTS.stops());

    return new WheelchairAccessibilityRequest(
      a.asBoolean("enabled", DEFAULTS.enabled()),
      trips,
      stops
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
