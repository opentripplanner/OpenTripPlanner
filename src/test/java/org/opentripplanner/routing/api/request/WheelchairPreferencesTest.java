package org.opentripplanner.routing.api.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.api.request.preference.AccessibilityPreferences.ofOnlyAccessible;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;
import org.opentripplanner.test.support.VariableSource;

class WheelchairPreferencesTest {

  static Stream<Arguments> testCases = Stream.of(
    Arguments.of(0.33333333333, 0.33, 0.333),
    Arguments.of(0.77777777777, 0.78, 0.778)
  );

  @ParameterizedTest(
    name = "Normalize value of {0} to rounded value {1} (maxSlope) and {2} (reluctance fields)"
  )
  @VariableSource("testCases")
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
}
