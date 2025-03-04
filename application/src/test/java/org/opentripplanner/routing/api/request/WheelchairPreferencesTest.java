package org.opentripplanner.routing.api.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.api.request.preference.WheelchairPreferences.DEFAULT;
import static org.opentripplanner.routing.api.request.preference.WheelchairPreferences.DEFAULT_COSTS;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;

class WheelchairPreferencesTest {

  static Stream<Arguments> roundingTestCases() {
    return Stream.of(
      Arguments.of(0.33333333333, 0.33, 0.333),
      Arguments.of(0.77777777777, 0.78, 0.778)
    );
  }

  @ParameterizedTest(
    name = "Normalize value of {0} to rounded value {1} (maxSlope) and {2} (reluctance fields)"
  )
  @MethodSource("roundingTestCases")
  void testConstructorNormalization(double raw, double rounded2, double rounded3) {
    var roundedRequest = WheelchairPreferences.of()
      .withTripOnlyAccessible()
      .withStopOnlyAccessible()
      .withElevatorOnlyAccessible()
      .withInaccessibleStreetReluctance(raw)
      .withStairsReluctance(raw)
      .withMaxSlope(raw)
      .withSlopeExceededReluctance(raw)
      .build();

    assertEquals(roundedRequest.maxSlope(), rounded3);
    assertEquals(roundedRequest.stairsReluctance(), rounded2);
    assertEquals(roundedRequest.inaccessibleStreetReluctance(), rounded2);
    assertEquals(roundedRequest.slopeExceededReluctance(), rounded2);
  }

  static Stream<Arguments> toStringTestCases() {
    return Stream.of(
      Arguments.of(DEFAULT, "WheelchairPreferences{}"),
      Arguments.of(
        WheelchairPreferences.of().withElevatorOnlyAccessible().build(),
        "WheelchairPreferences{elevator: OnlyConsiderAccessible}"
      ),
      Arguments.of(
        WheelchairPreferences.of().withTrip(DEFAULT_COSTS).build(),
        "WheelchairPreferences{trip: AccessibilityPreferences{}}"
      ),
      Arguments.of(
        WheelchairPreferences.of().withTrip(it -> it.withInaccessibleCost(100)).build(),
        "WheelchairPreferences{trip: AccessibilityPreferences{inaccessibleCost: $100}}"
      ),
      Arguments.of(
        WheelchairPreferences.of().withTripCost(99, 100).build(),
        "WheelchairPreferences{trip: AccessibilityPreferences{unknownCost: $99, inaccessibleCost: $100}}"
      ),
      Arguments.of(
        WheelchairPreferences.of()
          .withTripCost(10, 100)
          .withStopCost(20, 200)
          .withElevatorCost(30, 300)
          .withInaccessibleStreetReluctance(1.0)
          .withMaxSlope(0.123)
          .withSlopeExceededReluctance(3)
          .withStairsReluctance(4)
          .build(),
        "WheelchairPreferences{trip: AccessibilityPreferences{unknownCost: $10, inaccessibleCost: $100}, stop: AccessibilityPreferences{unknownCost: $20, inaccessibleCost: $200}, elevator: AccessibilityPreferences{unknownCost: $30, inaccessibleCost: $300}, inaccessibleStreetReluctance: 1.0, maxSlope: 0.123, slopeExceededReluctance: 3.0, stairsReluctance: 4.0}"
      )
    );
  }

  @ParameterizedTest(name = "Verify toString() value is {1}")
  @MethodSource("toStringTestCases")
  void testToString(WheelchairPreferences subject, String expected) {
    assertEquals(expected, subject.toString());
  }
}
