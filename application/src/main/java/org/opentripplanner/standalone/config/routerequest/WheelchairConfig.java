package org.opentripplanner.standalone.config.routerequest;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_0;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;

import org.opentripplanner.routing.api.request.preference.AccessibilityPreferences;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class WheelchairConfig {

  static boolean wheelchairEnabled(NodeAdapter root, String parameterName) {
    return WheelchairConfig.wheelchairRoot(root, parameterName)
      .of("enabled")
      .since(V2_0)
      .summary("Enable wheelchair accessibility.")
      .asBoolean(false);
  }

  public static void mapWheelchairPreferences(
    NodeAdapter root,
    WheelchairPreferences.Builder builder,
    String fieldName
  ) {
    var a = wheelchairRoot(root, fieldName);
    var dft = builder.original();

    builder
      .withTrip(it ->
        mapAccessibilityPreferences(
          a
            .of("trip")
            .since(V2_2)
            .summary("Configuration for when to use inaccessible trips.")
            .asObject(),
          it
        )
      )
      .withStop(it ->
        mapAccessibilityPreferences(
          a
            .of("stop")
            .since(V2_2)
            .summary("Configuration for when to use inaccessible stops.")
            .asObject(),
          it
        )
      )
      .withElevator(it ->
        mapAccessibilityPreferences(
          a
            .of("elevator")
            .since(V2_2)
            .summary("Configuration for when to use inaccessible elevators.")
            .asObject(),
          it
        )
      )
      .withInaccessibleStreetReluctance(
        a
          .of("inaccessibleStreetReluctance")
          .since(V2_2)
          .summary(
            "The factor to multiply the cost of traversing a street edge that is not wheelchair-accessible."
          )
          .asDouble(dft.inaccessibleStreetReluctance())
      )
      .withMaxSlope(
        a
          .of("maxSlope")
          .since(V2_0)
          .summary("The maximum slope as a fraction of 1.")
          .description("9 percent would be `0.09`")
          .asDouble(dft.maxSlope())
      )
      .withSlopeExceededReluctance(
        a
          .of("slopeExceededReluctance")
          .since(V2_2)
          .summary("How much streets with high slope should be avoided.")
          .description(
            """
            What factor should be given to street edges, which are over the
            max slope. The penalty is not static but scales with how much you
            exceed the maximum slope. Set to negative to disable routing on
            too steep edges.
            """
          )
          .asDouble(dft.slopeExceededReluctance())
      )
      .withStairsReluctance(
        a
          .of("stairsReluctance")
          .since(V2_2)
          .summary("How much stairs should be avoided.")
          .description(
            """
            Stairs are not completely excluded for wheelchair users but
            severely punished. This value determines how much they are
            punished. This should be a very high value as you want to only
            include stairs as a last result."""
          )
          .asDouble(dft.stairsReluctance())
      );
  }

  static NodeAdapter wheelchairRoot(NodeAdapter root, String parameterName) {
    return root
      .of(parameterName)
      .since(V2_2)
      .summary("See [Wheelchair Accessibility](Accessibility.md)")
      .asObject();
  }

  static void mapAccessibilityPreferences(
    NodeAdapter adapter,
    AccessibilityPreferences.Builder builder
  ) {
    var onlyConsiderAccessible = adapter
      .of("onlyConsiderAccessible")
      .since(V2_2)
      .summary(
        "Whether to only use this entity if it is explicitly marked as wheelchair accessible."
      )
      .asBoolean(builder.onlyConsiderAccessible());

    var unknownCost = adapter
      .of("unknownCost")
      .since(V2_2)
      .summary("The cost to add when traversing an entity with unknown accessibility information.")
      .asInt(builder.unknownCost());

    var inaccessibleCost = adapter
      .of("inaccessibleCost")
      .since(V2_2)
      .summary("The cost to add when traversing an entity which is know to be inaccessible.")
      .asInt(builder.inaccessibleCost());

    if (
      !adapter.exist("onlyConsiderAccessible") &&
      (adapter.exist("unknownCost") || adapter.exist("inaccessibleCost"))
    ) {
      onlyConsiderAccessible = false;
    }

    if (
      onlyConsiderAccessible && (adapter.exist("unknownCost") || adapter.exist("inaccessibleCost"))
    ) {
      throw new IllegalStateException(
        "If `onlyConsiderAccessible` is set then `unknownCost` and `inaccessibleCost` may not be set at " +
        adapter.contextPath()
      );
    }

    if (onlyConsiderAccessible) {
      builder.withAccessibleOnly();
    } else {
      builder.withUnknownCost(unknownCost).withInaccessibleCost(inaccessibleCost);
    }
  }
}
