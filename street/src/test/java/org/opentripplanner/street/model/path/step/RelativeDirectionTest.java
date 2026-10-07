package org.opentripplanner.model.plan.walkstep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.walkstep.RelativeDirection.CIRCLE_CLOCKWISE;
import static org.opentripplanner.model.plan.walkstep.RelativeDirection.CIRCLE_COUNTERCLOCKWISE;
import static org.opentripplanner.model.plan.walkstep.RelativeDirection.CONTINUE;
import static org.opentripplanner.model.plan.walkstep.RelativeDirection.HARD_LEFT;
import static org.opentripplanner.model.plan.walkstep.RelativeDirection.HARD_RIGHT;
import static org.opentripplanner.model.plan.walkstep.RelativeDirection.LEFT;
import static org.opentripplanner.model.plan.walkstep.RelativeDirection.RIGHT;
import static org.opentripplanner.model.plan.walkstep.RelativeDirection.SLIGHTLY_LEFT;
import static org.opentripplanner.model.plan.walkstep.RelativeDirection.SLIGHTLY_RIGHT;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RelativeDirectionTest {

  @SuppressWarnings("unused")
  static Stream<Arguments> testCasesNormal() {
    return Stream.of(
      // Turn Right
      tc(0, CONTINUE),
      tc(17, CONTINUE),
      tc(18, SLIGHTLY_RIGHT),
      tc(40, SLIGHTLY_RIGHT),
      tc(41, RIGHT),
      tc(114, RIGHT),
      tc(115, HARD_RIGHT),
      tc(179, HARD_RIGHT),
      tc(180, HARD_LEFT),
      // Turn Left
      tc(0, CONTINUE),
      tc(-17, CONTINUE),
      tc(-18, SLIGHTLY_LEFT),
      tc(-40, SLIGHTLY_LEFT),
      tc(-41, LEFT),
      tc(-114, LEFT),
      tc(-115, HARD_LEFT),
      tc(-179, HARD_LEFT),
      tc(-180, HARD_LEFT),
      tc(-181, HARD_RIGHT)
    );
  }

  @ParameterizedTest(name = "Turning {0} degrees should give a relative direction of {1}")
  @MethodSource("testCasesNormal")
  void testCalculateForNormalIntersections(int thisAngleDegrees, RelativeDirection expected) {
    assertEquals(expected, RelativeDirection.calculate(angle(thisAngleDegrees), false));
  }

  @Test
  void testCalculateTwoRelativeAngles() {
    assertEquals(SLIGHTLY_RIGHT, RelativeDirection.calculate(angle(100), angle(140), false));
    assertEquals(RIGHT, RelativeDirection.calculate(angle(100), angle(141), false));
    assertEquals(SLIGHTLY_LEFT, RelativeDirection.calculate(angle(100), angle(60), false));
    assertEquals(LEFT, RelativeDirection.calculate(angle(100), angle(59), false));
  }

  @Test
  void testCalculateForRoundabouts() {
    assertEquals(CIRCLE_COUNTERCLOCKWISE, RelativeDirection.calculate(angle(0), true));
    assertEquals(CIRCLE_COUNTERCLOCKWISE, RelativeDirection.calculate(angle(67), true));
    assertEquals(CIRCLE_COUNTERCLOCKWISE, RelativeDirection.calculate(angle(180), true));
    assertEquals(CIRCLE_CLOCKWISE, RelativeDirection.calculate(angle(-1), true));
    assertEquals(CIRCLE_CLOCKWISE, RelativeDirection.calculate(angle(-102), true));
    assertEquals(CIRCLE_CLOCKWISE, RelativeDirection.calculate(angle(-179), true));
  }

  private static double angle(int degree) {
    return 2 * Math.PI * (degree / 360.0);
  }

  static Arguments tc(int angleInDegrees, RelativeDirection expected) {
    return Arguments.of(angleInDegrees, expected);
  }
}
