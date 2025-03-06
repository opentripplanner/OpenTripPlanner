package org.opentripplanner.routing.util.elevation;

import static java.util.Locale.ROOT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.util.elevation.ToblersHikingFunctionTest.TestCase.tc;

import org.junit.jupiter.api.Test;

public class ToblersHikingFunctionTest {

  private static final double CUT_OFF_LIMIT = 3.2;

  @Test
  public void calculateHorizontalWalkingDistanceMultiplier() {
    TestCase[] testCases = {
      tc(35, CUT_OFF_LIMIT),
      tc(31.4, 3.0),
      tc(30, 2.86),
      tc(25, 2.40),
      tc(20, 2.02),
      tc(15, 1.69),
      tc(10, 1.42),
      tc(5, 1.19),
      tc(0, 1.00),
      tc(-5, 0.84),
      tc(-10, 1.00),
      tc(-15, 1.19),
      tc(-20, 1.42),
      tc(-25, 1.69),
      tc(-30, 2.02),
      tc(-35, 2.40),
      tc(-40, 2.86),
      tc(-45, CUT_OFF_LIMIT),
    };

    ToblersHikingFunction f = new ToblersHikingFunction(CUT_OFF_LIMIT);

    for (TestCase it : testCases) {
      double distMultiplier = f.calculateHorizontalWalkingDistanceMultiplier(it.dx, it.dh);
      assertEquals(it.expected, distMultiplier, 0.01, it.describe());
    }
  }

  static class TestCase {

    final double slopeAnglePercentage, dx, dh, expected;

    TestCase(double slopeAnglePercentage, double expected) {
      this.slopeAnglePercentage = slopeAnglePercentage;
      // Set the horizontal distance to 300 meters
      this.dx = 300.0;
      // Calculate the height:
      this.dh = (dx * slopeAnglePercentage) / 100.0;

      this.expected = expected;
    }

    static TestCase tc(double slopeAngle, double expected) {
      return new TestCase(slopeAngle, expected);
    }

    String describe() {
      return String.format(
        ROOT,
        "Multiplier at %.1f%% slope angle with dx %.1f and dh %.1f.",
        slopeAnglePercentage,
        dx,
        dh
      );
    }
  }
}
