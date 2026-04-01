package org.opentripplanner.osm.wayproperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.of;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SpeedParserTest {

  private static final float EPSILON = 0.01f;

  private static List<Arguments> speedParsingTestCases() {
    return List.of(
      of(1.3889, "5"),
      of(1.3889, "5 kmh"),
      of(1.3889, " 5 kmh "),
      of(1.3889, " 5 "),
      of(4.1666, "15"),
      of(4.3055, "15.5"),
      of(4.3055, "15.5 kmh"),
      of(4.3055, "15.5 kph"),
      of(4.3055, "15.5 km/h"),
      of(22.347, "50 mph"),
      of(22.347, "50.0 mph"),
      of(25.722, "50 knots")
    );
  }

  @ParameterizedTest
  @MethodSource("speedParsingTestCases")
  void speedParsing(double expected, String input) {
    assertEquals(expected, SpeedParser.getMetersSecondFromSpeed(input), EPSILON);
  }
}
