package org.opentripplanner.standalone.config;

import static org.opentripplanner.routing.api.request.preference.WheelchairPreferences.DEFAULT;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import org.opentripplanner.routing.api.request.preference.AccessibilityPreferences;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class WheelchairAccessibilityRequestMapper {

  static WheelchairPreferences mapAccessibilityRequest(NodeAdapter a) {
    return new WheelchairPreferences(
      mapAccessibilityFeature(a.path("trip"), DEFAULT.trip()),
      mapAccessibilityFeature(a.path("stop"), DEFAULT.stop()),
      mapAccessibilityFeature(a.path("elevator"), DEFAULT.elevator()),
      a
        .of("inaccessibleStreetReluctance")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .asDouble(DEFAULT.inaccessibleStreetReluctance()),
      a.of("maxSlope").withDoc(NA, /*TODO DOC*/"TODO").asDouble(DEFAULT.maxSlope()),
      a
        .of("slopeExceededReluctance")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .asDouble(DEFAULT.slopeExceededReluctance()),
      a.of("stairsReluctance").withDoc(NA, /*TODO DOC*/"TODO").asDouble(DEFAULT.stairsReluctance())
    );
  }

  private static AccessibilityPreferences mapAccessibilityFeature(
    NodeAdapter adapter,
    AccessibilityPreferences defaultValue
  ) {
    var onlyAccessible = adapter
      .of("onlyConsiderAccessible")
      .withDoc(NA, /*TODO DOC*/"TODO")
      .asBoolean(defaultValue.onlyConsiderAccessible());

    var unknownCost = adapter.asInt("unknownCost", 60 * 10);
    var inaccessibleCost = adapter.asInt("inaccessibleCost", 60 * 60);
    if (onlyAccessible) {
      return AccessibilityPreferences.ofOnlyAccessible();
    } else {
      return AccessibilityPreferences.ofCost(unknownCost, inaccessibleCost);
    }
  }
}
