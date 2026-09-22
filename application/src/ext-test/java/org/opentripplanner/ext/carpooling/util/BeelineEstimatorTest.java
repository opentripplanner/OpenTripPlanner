package org.opentripplanner.ext.carpooling.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;

class BeelineEstimatorTest {

  @Test
  void estimateDurationDividesBeelineDistanceBySpeed() {
    double speed = 20.0;
    double distance = SphericalDistanceLibrary.fastDistance(
      OSLO_CENTER.asJtsCoordinate(),
      OSLO_NORTH.asJtsCoordinate()
    );

    assertEquals(
      Duration.ofSeconds((long) (distance / speed)),
      new BeelineEstimator(speed).estimateDuration(OSLO_CENTER, OSLO_NORTH)
    );
  }

  @Test
  void estimateDurationIsZeroBetweenIdenticalPoints() {
    assertEquals(Duration.ZERO, new BeelineEstimator().estimateDuration(OSLO_CENTER, OSLO_CENTER));
  }

  @Test
  void noArgConstructorUsesDefaultSpeed() {
    assertEquals(BeelineEstimator.DEFAULT_SPEED_MPS, new BeelineEstimator().getSpeed());
  }

  @Test
  void constructorRejectsNonPositiveSpeed() {
    assertThrows(IllegalArgumentException.class, () -> new BeelineEstimator(0.0));
    assertThrows(IllegalArgumentException.class, () -> new BeelineEstimator(-5.0));
  }
}
