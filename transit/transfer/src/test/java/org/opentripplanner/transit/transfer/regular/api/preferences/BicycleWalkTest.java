package org.opentripplanner.transit.transfer.regular.api.preferences;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.basic.Speed;

class BicycleWalkTest {

  private static final BicycleWalk SUBJECT = BicycleWalk.of(
    Speed.ofMetersPerSecond(1.2),
    Reluctance.of(5.0)
  );

  @Test
  void keepsTheValues() {
    assertThat(SUBJECT.speed()).isEqualTo(Speed.ofMetersPerSecond(1.2));
    assertThat(SUBJECT.reluctance()).isEqualTo(Reluctance.of(5.0));
  }

  @Test
  void equalValuesAreEqual() {
    var same = BicycleWalk.of(Speed.ofMetersPerSecond(1.2), Reluctance.of(5.0));

    assertThat(same).isEqualTo(SUBJECT);
    assertThat(same.hashCode()).isEqualTo(SUBJECT.hashCode());
    assertThat(BicycleWalk.of(Speed.ofMetersPerSecond(1.5), Reluctance.of(5.0))).isNotEqualTo(
      SUBJECT
    );
    assertThat(BicycleWalk.of(Speed.ofMetersPerSecond(1.2), Reluctance.of(6.0))).isNotEqualTo(
      SUBJECT
    );
  }

  @Test
  void theDistanceIsTheSumOfTheDistancesOfTheSpeedAndTheReluctance() {
    var other = BicycleWalk.of(Speed.ofMetersPerSecond(1.5), Reluctance.of(6.0));

    assertThat(SUBJECT.normalizedDistance(SUBJECT)).isEqualTo(0.0);
    assertThat(SUBJECT.normalizedDistance(other)).isEqualTo(
      SUBJECT.speed().normalizedDistance(other.speed()) +
        SUBJECT.reluctance().normalizedDistance(other.reluctance())
    );
  }
}
