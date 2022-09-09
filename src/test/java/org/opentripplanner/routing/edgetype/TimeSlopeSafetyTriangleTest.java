package org.opentripplanner.routing.edgetype;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.routing.api.request.preference.TimeSlopeSafetyTriangle;
import org.opentripplanner.test.support.VariableSource;

public class TimeSlopeSafetyTriangleTest {

  float DELTA = 0.001f;

  @SuppressWarnings("unused")
  static Stream<Arguments> testCases = Stream.of(
    // Input: time | slope | safety || Expected: time | slope | safety
    Arguments.of(0.5, 0.3, 0.2, 0.5, 0.3, 0.2, "Exact"),
    Arguments.of(1d, 1d, 1d, 0.33, 0.33, 0.34, "Greater than 1"),
    Arguments.of(30d, 10d, 20d, 0.5, 0.17, 0.33, "Greater than 1 - big"),
    Arguments.of(1d, 0d, 0d, 1d, 0d, 0d, "Two zeros"),
    Arguments.of(0d, 0d, 0d, 0.33, 0.33, 0.34, "All zeros"),
    Arguments.of(0.1, -1d, -1d, 1d, 0d, 0d, "Less than zero"),
    Arguments.of(0d, 0.07, 0.93, 0d, 0.07, 0.93, "None precise round-off: " + (1.0 - 0.07))
  );

  @ParameterizedTest(name = "Time/slope/safety: | {0} {1} {2} || {3} {4} {5} |  {6}")
  @VariableSource("testCases")
  public void test(
    double inTi,
    double inSl,
    double inSa,
    double expTi,
    double expSl,
    double expSa,
    String description
  ) {
    var subject = new TimeSlopeSafetyTriangle(inTi, inSl, inSa);
    assertEquals(expTi, subject.time(), DELTA, description);
    assertEquals(expSl, subject.slope(), DELTA, description);
    assertEquals(expSa, subject.safety(), DELTA, description);
  }

  @Test
  public void testLessThanZero() {
    var subject = new TimeSlopeSafetyTriangle(0.1, -1, -1);
    assertEquals(1, subject.time(), DELTA);
    assertEquals(0, subject.slope(), DELTA);
    assertEquals(0, subject.safety(), DELTA);
  }
}
