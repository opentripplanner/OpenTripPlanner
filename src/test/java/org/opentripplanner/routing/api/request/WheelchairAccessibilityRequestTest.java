package org.opentripplanner.routing.api.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.api.request.WheelchairAccessibilityFeature.ofOnlyAccessible;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.test.support.VariableSource;

class WheelchairAccessibilityRequestTest {

  static Stream<Arguments> testCases = Stream.of(
    Arguments.of(0.08333333, 0.083),
    Arguments.of(0.083, 0.083),
    Arguments.of(0.0834, 0.083),
    Arguments.of(0.0835, 0.084),
    Arguments.of(0.11111, 0.111)
  );

  @ParameterizedTest(name = "maxSlope of {0} should be rounded to {1}")
  @VariableSource("testCases")
  void shouldRoundTo3DecimalPlaces(double raw, double rounded) {
    var roundedRequest = new WheelchairAccessibilityRequest(
      true,
      ofOnlyAccessible(),
      ofOnlyAccessible(),
      ofOnlyAccessible(),
      1,
      raw,
      1,
      1
    )
      .round();

    assertEquals(roundedRequest.maxSlope(), rounded);
  }
}
