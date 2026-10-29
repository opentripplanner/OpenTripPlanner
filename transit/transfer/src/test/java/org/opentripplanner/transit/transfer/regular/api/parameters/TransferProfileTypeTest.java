package org.opentripplanner.transit.transfer.regular.api.parameters;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType.BICYCLE;
import static org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType.CAR;
import static org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType.WALK;
import static org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType.WHEELCHAIR;

import org.junit.jupiter.api.Test;

class TransferProfileTypeTest {

  @Test
  void theDistanceToTheSameTypeIsZero() {
    for (var type : TransferProfileType.values()) {
      assertThat(type.normalizedDistance(type)).isEqualTo(0.0);
    }
  }

  @Test
  void theDistanceIsTheSameInBothDirections() {
    for (var a : TransferProfileType.values()) {
      for (var b : TransferProfileType.values()) {
        assertThat(a.normalizedDistance(b)).isEqualTo(b.normalizedDistance(a));
      }
    }
  }

  @Test
  void theDistancesBetweenTheTypes() {
    assertThat(WHEELCHAIR.normalizedDistance(WALK)).isEqualTo(1000.0);
    assertThat(WALK.normalizedDistance(BICYCLE)).isEqualTo(2000.0);
    assertThat(WHEELCHAIR.normalizedDistance(BICYCLE)).isEqualTo(3000.0);
    assertThat(BICYCLE.normalizedDistance(CAR)).isEqualTo(4000.0);
    assertThat(WALK.normalizedDistance(CAR)).isEqualTo(6000.0);
    assertThat(WHEELCHAIR.normalizedDistance(CAR)).isEqualTo(7000.0);
  }

  @Test
  void aWheelchairIsClosestToTheWalkAndABicycleIsClosestToTheWalkToo() {
    assertThat(WHEELCHAIR.normalizedDistance(WALK)).isLessThan(
      WHEELCHAIR.normalizedDistance(BICYCLE)
    );
    assertThat(BICYCLE.normalizedDistance(WALK)).isLessThan(BICYCLE.normalizedDistance(WHEELCHAIR));
    assertThat(BICYCLE.normalizedDistance(WALK)).isLessThan(BICYCLE.normalizedDistance(CAR));
  }
}
