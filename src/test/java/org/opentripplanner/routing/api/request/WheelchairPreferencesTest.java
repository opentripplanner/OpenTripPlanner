package org.opentripplanner.routing.api.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.api.request.preference.AccessibilityPreferences.ofCost;
import static org.opentripplanner.routing.api.request.preference.AccessibilityPreferences.ofOnlyAccessible;
import static org.opentripplanner.routing.api.request.preference.WheelchairPreferences.DEFAULT;
import static org.opentripplanner.routing.api.request.preference.WheelchairPreferences.DEFAULT_COSTS;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;
import org.opentripplanner.test.support.VariableSource;

class WheelchairPreferencesTest {

  static Stream<Arguments> roundingTestCases = Stream.of(
    Arguments.of(0.33333333333, 0.33, 0.333),
    Arguments.of(0.77777777777, 0.78, 0.778)
  );

  @ParameterizedTest(
    name = "Normalize value of {0} to rounded value {1} (maxSlope) and {2} (reluctance fields)"
  )
  @VariableSource("roundingTestCases")
  void testConstructorNormalization(double raw, double rounded2, double rounded3) {
    var roundedRequest = new WheelchairPreferences(
      ofOnlyAccessible(),
      ofOnlyAccessible(),
      ofOnlyAccessible(),
      raw,
      raw,
      raw,
      raw
    );

    assertEquals(roundedRequest.maxSlope(), rounded3);
    assertEquals(roundedRequest.stairsReluctance(), rounded2);
    assertEquals(roundedRequest.inaccessibleStreetReluctance(), rounded2);
    assertEquals(roundedRequest.slopeExceededReluctance(), rounded2);
  }

  static Stream<Arguments> toStringTestCases = Stream.of(
    Arguments.of(DEFAULT, "WheelchairPreferences{}"),
    Arguments.of(
      new WheelchairPreferences(
        DEFAULT.trip(),
        DEFAULT.stop(),
        ofOnlyAccessible(),
        DEFAULT.inaccessibleStreetReluctance(),
        DEFAULT.maxSlope(),
        DEFAULT.slopeExceededReluctance(),
        DEFAULT.stairsReluctance()
      ),
      "WheelchairPreferences{elevator: OnlyConsiderAccessible}"
    ),
    Arguments.of(
      new WheelchairPreferences(
        DEFAULT_COSTS,
        DEFAULT.stop(),
        DEFAULT.elevator(),
        DEFAULT.inaccessibleStreetReluctance(),
        DEFAULT.maxSlope(),
        DEFAULT.slopeExceededReluctance(),
        DEFAULT.stairsReluctance()
      ),
      "WheelchairPreferences{trip: AccessibilityPreferences{}}"
    ),
    Arguments.of(
      new WheelchairPreferences(
        ofCost(DEFAULT_COSTS.unknownCost(), 100),
        DEFAULT.stop(),
        DEFAULT.elevator(),
        DEFAULT.inaccessibleStreetReluctance(),
        DEFAULT.maxSlope(),
        DEFAULT.slopeExceededReluctance(),
        DEFAULT.stairsReluctance()
      ),
      "WheelchairPreferences{trip: AccessibilityPreferences{inaccessibleCost: $100}}"
    ),
    Arguments.of(
      new WheelchairPreferences(
        ofCost(99, 100),
        DEFAULT.stop(),
        DEFAULT.elevator(),
        DEFAULT.inaccessibleStreetReluctance(),
        DEFAULT.maxSlope(),
        DEFAULT.slopeExceededReluctance(),
        DEFAULT.stairsReluctance()
      ),
      "WheelchairPreferences{trip: AccessibilityPreferences{unknownCost: $99, inaccessibleCost: $100}}"
    ),
    Arguments.of(
      new WheelchairPreferences(ofCost(10, 100), ofCost(20, 200), ofCost(30, 300), 1, 0.123, 3, 4),
      "WheelchairPreferences{trip: AccessibilityPreferences{unknownCost: $10, inaccessibleCost: $100}, stop: AccessibilityPreferences{unknownCost: $20, inaccessibleCost: $200}, elevator: AccessibilityPreferences{unknownCost: $30, inaccessibleCost: $300}, inaccessibleStreetReluctance: 1.0, maxSlope: 0.123, slopeExceededReluctance: 3.0, stairsReluctance: 4.0}"
    )
  );

  @ParameterizedTest(name = "Verify toString() value is {1}")
  @VariableSource("toStringTestCases")
  void testToString(WheelchairPreferences subject, String expected) {
    assertEquals(expected, subject.toString());
  }
}
