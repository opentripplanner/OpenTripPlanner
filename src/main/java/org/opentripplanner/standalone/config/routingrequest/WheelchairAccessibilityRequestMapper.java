package org.opentripplanner.standalone.config.routingrequest;

import static org.opentripplanner.routing.api.request.preference.WheelchairPreferences.DEFAULT;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import org.opentripplanner.routing.api.request.preference.AccessibilityPreferences;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class WheelchairAccessibilityRequestMapper {

  static WheelchairPreferences mapAccessibilityRequest(NodeAdapter a) {
    return new WheelchairPreferences(
      mapAccessibilityFeature(
        a.of("trip").since(NA).summary("TODO").description(/*TODO DOC*/"TODO").asObject(),
        DEFAULT.trip()
      ),
      mapAccessibilityFeature(
        a.of("stop").since(NA).summary("TODO").description(/*TODO DOC*/"TODO").asObject(),
        DEFAULT.stop()
      ),
      mapAccessibilityFeature(
        a.of("elevator").since(NA).summary("TODO").description(/*TODO DOC*/"TODO").asObject(),
        DEFAULT.elevator()
      ),
      a
        .of("inaccessibleStreetReluctance")
        .since(NA)
        .summary("TODO")
        .asDouble(DEFAULT.inaccessibleStreetReluctance()),
      a.of("maxSlope").since(NA).summary("TODO").asDouble(DEFAULT.maxSlope()),
      a
        .of("slopeExceededReluctance")
        .since(NA)
        .summary("TODO")
        .asDouble(DEFAULT.slopeExceededReluctance()),
      a.of("stairsReluctance").since(NA).summary("TODO").asDouble(DEFAULT.stairsReluctance())
    );
  }

  private static AccessibilityPreferences mapAccessibilityFeature(
    NodeAdapter adapter,
    AccessibilityPreferences defaultValue
  ) {
    var onlyAccessible = adapter
      .of("onlyConsiderAccessible")
      .since(NA)
      .summary("TODO")
      .asBoolean(defaultValue.onlyConsiderAccessible());

    var unknownCost = adapter.of("unknownCost").since(NA).summary("TODO").asInt(60 * 10);
    var inaccessibleCost = adapter.of("inaccessibleCost").since(NA).summary("TODO").asInt(60 * 60);
    if (onlyAccessible) {
      return AccessibilityPreferences.ofOnlyAccessible();
    } else {
      return AccessibilityPreferences.ofCost(unknownCost, inaccessibleCost);
    }
  }
}
