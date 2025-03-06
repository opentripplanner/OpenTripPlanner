package org.opentripplanner.standalone.config.routerequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.routing.api.request.preference.AccessibilityPreferences.ofCost;
import static org.opentripplanner.routing.api.request.preference.AccessibilityPreferences.ofOnlyAccessible;
import static org.opentripplanner.routing.api.request.preference.WheelchairPreferences.DEFAULT_COSTS;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.newNodeAdapterForTest;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.routing.api.request.preference.AccessibilityPreferences;

class WheelchairConfigTest {

  static Stream<Arguments> mapAccessibilityPreferencesTestCases() {
    return Stream.of(
      Arguments.of(
        "default ofOnlyAccessible()",
        "{}",
        ofOnlyAccessible(),
        DEFAULT_COSTS,
        ofOnlyAccessible()
      ),
      Arguments.of("default cost", "{}", ofCost(100, 200), ofCost(100, 200), ofCost(100, 200)),
      Arguments.of(
        "onlyConsiderAccessible with default costs",
        "{\"onlyConsiderAccessible\": true}",
        DEFAULT_COSTS,
        DEFAULT_COSTS,
        ofOnlyAccessible()
      ),
      Arguments.of(
        "Default costs with default ofOnlyAccessible()",
        "{\"onlyConsiderAccessible\": false}",
        ofOnlyAccessible(),
        DEFAULT_COSTS,
        DEFAULT_COSTS
      ),
      Arguments.of(
        "Only unknownCost set with default ofOnlyAccessible()",
        "{\"unknownCost\": 100}",
        ofOnlyAccessible(),
        DEFAULT_COSTS,
        ofCost(100, DEFAULT_COSTS.inaccessibleCost())
      ),
      Arguments.of(
        "Only inaccessibleCost set with default ofOnlyAccessible()",
        "{\"inaccessibleCost\": 100}",
        ofOnlyAccessible(),
        DEFAULT_COSTS,
        ofCost(DEFAULT_COSTS.unknownCost(), 100)
      ),
      Arguments.of(
        "All values set",
        "{\"unknownCost\": 200, \"inaccessibleCost\": 100, \"onlyConsiderAccessible\": false}",
        ofOnlyAccessible(),
        DEFAULT_COSTS,
        ofCost(200, 100)
      )
    );
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("mapAccessibilityPreferencesTestCases")
  void testMapAccessibilityPreferences(
    String name,
    String json,
    AccessibilityPreferences defaultValue,
    AccessibilityPreferences defaultCost,
    AccessibilityPreferences expected
  ) {
    var nodeAdapter = newNodeAdapterForTest(json);
    var builder = defaultValue.copyOfWithDefaultCosts(defaultCost);

    WheelchairConfig.mapAccessibilityPreferences(nodeAdapter, builder);

    assertEquals(expected, builder.build());
  }

  @Test
  void testMapAccessibilityWithIncompatibleValues() {
    var nodeAdapter = newNodeAdapterForTest(
      "{\"unknownCost\": 200, \"inaccessibleCost\": 100, \"onlyConsiderAccessible\": true}"
    );

    assertThrows(IllegalStateException.class, () -> {
      var builder = ofOnlyAccessible().copyOfWithDefaultCosts(DEFAULT_COSTS);
      WheelchairConfig.mapAccessibilityPreferences(nodeAdapter, builder);
    });
  }
}
