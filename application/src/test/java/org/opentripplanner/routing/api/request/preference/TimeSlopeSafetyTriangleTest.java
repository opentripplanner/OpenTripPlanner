package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TimeSlopeSafetyTriangleTest {

  float DELTA = 0.001f;

  @SuppressWarnings("unused")
  static Stream<Arguments> testCases() {
    return Stream.of(
      // Input: time | slope | safety || Expected: time | slope | safety
      Arguments.of(0.5, 0.3, 0.2, 0.5, 0.3, 0.2, "Exact"),
      Arguments.of(1d, 1d, 1d, 0.33, 0.33, 0.34, "Greater than 1"),
      Arguments.of(30d, 10d, 20d, 0.5, 0.17, 0.33, "Greater than 1 - big"),
      Arguments.of(1d, 0d, 0d, 1d, 0d, 0d, "Two zeros"),
      Arguments.of(0d, 0d, 0d, 0.33, 0.33, 0.34, "All zeros"),
      Arguments.of(0.1, -1d, -1d, 1d, 0d, 0d, "Less than zero"),
      Arguments.of(0d, 0.07, 0.93, 0d, 0.07, 0.93, "None precise round-off: " + (1.0 - 0.07))
    );
  }

  @ParameterizedTest(name = "Time/slope/safety: | {0} {1} {2} || {3} {4} {5} |  {6}")
  @MethodSource("testCases")
  public void test(
    double inTime,
    double inSlope,
    double inSafety,
    double expTime,
    double expSlope,
    double expSafety,
    String description
  ) {
    var subject = TimeSlopeSafetyTriangle.of()
      .withTime(inTime)
      .withSlope(inSlope)
      .withSafety(inSafety)
      .build();
    assertEquals(expTime, subject.time(), DELTA, description);
    assertEquals(expSlope, subject.slope(), DELTA, description);
    assertEquals(expSafety, subject.safety(), DELTA, description);
  }

  @Test
  public void testBuildWithDefaultValue() {
    // Set som arbitrary values for the default instance
    var expected = TimeSlopeSafetyTriangle.of()
      .withTime(1.0)
      .withSlope(2.0)
      .withSafety(3.0)
      .build();
    // then the default should be returned if no value is set
    var result = TimeSlopeSafetyTriangle.of().buildOrDefault(expected);

    assertSame(expected, result);
  }

  @Test
  public void testLessThanZero() {
    var subject = new TimeSlopeSafetyTriangle(0.1, -1, -1);
    assertEquals(1, subject.time(), DELTA);
    assertEquals(0, subject.slope(), DELTA);
    assertEquals(0, subject.safety(), DELTA);
  }
}
