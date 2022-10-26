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
        a
          .of("trip")
          .since(NA)
          .summary("Configuration for when to use inaccessible trips.")
          .asObject(),
        DEFAULT.trip()
      ),
      mapAccessibilityFeature(
        a
          .of("stop")
          .since(NA)
          .summary("Configuration for when to use inaccessible stops.")
          .asObject(),
        DEFAULT.stop()
      ),
      mapAccessibilityFeature(
        a
          .of("elevator")
          .since(NA)
          .summary("Configuration for when to use inaccessible elevators.")
          .asObject(),
        DEFAULT.elevator()
      ),
      a
        .of("inaccessibleStreetReluctance")
        .since(NA)
        .summary(
          "The factor to to multiply the cost of traversing a street edge that is not wheelchair-accessible."
        )
        .asDouble(DEFAULT.inaccessibleStreetReluctance()),
      a
        .of("maxSlope")
        .since(NA)
        .summary("The maximum slope as a fraction of 1.")
        .description("9 percent would be `0.09`")
        .asDouble(DEFAULT.maxSlope()),
      a
        .of("slopeExceededReluctance")
        .since(NA)
        .summary("How much streets with high slope should be avoided.")
        .description(
          """
            What factor should be given to street edges, which are over the
            max slope. The penalty is not static but scales with how much you
            exceed the maximum slope. Set to negative to disable routing on
            too steep edges.
            """
        )
        .asDouble(DEFAULT.slopeExceededReluctance()),
      a
        .of("stairsReluctance")
        .since(NA)
        .summary("How much stairs should be avoided.")
        .description(
          """
            Stairs are not completely excluded for wheelchair users but
            severely punished. This value determines how much they are
            punished. This should be a very high value as you want to only
            include stairs as a last result."""
        )
        .asDouble(DEFAULT.stairsReluctance())
    );
  }

  private static AccessibilityPreferences mapAccessibilityFeature(
    NodeAdapter adapter,
    AccessibilityPreferences defaultValue
  ) {
    var onlyAccessible = adapter
      .of("onlyConsiderAccessible")
      .since(NA)
      .summary(
        "Wheter to only use this entity if it is explicitly marked as wheelchair accessible."
      )
      .asBoolean(defaultValue.onlyConsiderAccessible());

    var unknownCost = adapter
      .of("unknownCost")
      .since(NA)
      .summary("The cost to add when traversing an entity with unknown accessibility information.")
      .asInt(60 * 10);
    var inaccessibleCost = adapter
      .of("inaccessibleCost")
      .since(NA)
      .summary("The cost to add when traversing an entity which is know to be inaccessible.")
      .asInt(60 * 60);
    if (onlyAccessible) {
      return AccessibilityPreferences.ofOnlyAccessible();
    } else {
      return AccessibilityPreferences.ofCost(unknownCost, inaccessibleCost);
    }
  }
}
