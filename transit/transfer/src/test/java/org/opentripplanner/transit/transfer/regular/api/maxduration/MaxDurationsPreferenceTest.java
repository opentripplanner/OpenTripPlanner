package org.opentripplanner.transit.transfer.regular.api.maxduration;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;

class MaxDurationsPreferenceTest {

  private static final Set<AllowedMode> NONE = Set.of();
  private static final Set<AllowedMode> CAR = Set.of(AllowedMode.CAR);
  private static final Set<AllowedMode> BICYCLE = Set.of(AllowedMode.BICYCLE);
  private static final Set<AllowedMode> BICYCLE_AND_CAR = Set.of(
    AllowedMode.BICYCLE,
    AllowedMode.CAR
  );

  private static final int MIN_30 = 30 * 60;
  private static final int MIN_40 = 40 * 60;
  private static final int MIN_45 = 45 * 60;
  private static final int MIN_60 = 60 * 60;

  private static final Duration D30 = Duration.ofMinutes(30);
  private static final Duration D45 = Duration.ofMinutes(45);
  private static final Duration D60 = Duration.ofMinutes(60);

  private static final Duration D20 = Duration.ofMinutes(20);

  /** All stops 20 min, bicycle stops 45 min, car stops 60 min. */
  private static final BicycleMaxDurations BICYCLE_ALL = new BicycleMaxDurations(D20, D45, D60);

  @Test
  void keepsTheMaxDuration() {
    assertThat(MaxDurationsPreference.of(Duration.ofMinutes(45)).longestDuration()).isEqualTo(D45);
  }

  @Test
  void equalDurationsAreEqual() {
    assertThat(MaxDurationsPreference.of(D30)).isEqualTo(MaxDurationsPreference.DEFAULT);
  }

  @Test
  void theMaxDurationMustBePositive() {
    assertThrows(IllegalArgumentException.class, () -> MaxDurationsPreference.of(Duration.ZERO));
    assertThrows(IllegalArgumentException.class, () ->
      MaxDurationsPreference.of(Duration.ofMinutes(-1))
    );
    assertThrows(IllegalArgumentException.class, () -> new CarMaxDuration(Duration.ZERO));
    assertThrows(IllegalArgumentException.class, () ->
      new BicycleMaxDurations(Duration.ZERO, null, null)
    );
  }

  @Test
  void theDefaultIsThirtyMinutesInTheWayOfTheType() {
    assertThat(MaxDurationsPreference.defaultFor(TransferProfileType.WALK)).isEqualTo(
      new PlainMaxDuration(D30)
    );
    assertThat(MaxDurationsPreference.defaultFor(TransferProfileType.WHEELCHAIR)).isEqualTo(
      new PlainMaxDuration(D30)
    );
    assertThat(MaxDurationsPreference.defaultFor(TransferProfileType.BICYCLE)).isEqualTo(
      new BicycleMaxDurations(null, D30, null)
    );
    assertThat(MaxDurationsPreference.defaultFor(TransferProfileType.CAR)).isEqualTo(
      new CarMaxDuration(D30)
    );
  }

  @Test
  void plainIsTheSameBetweenAllStops() {
    var subject = new PlainMaxDuration(D30);
    assertThat(subject.searchLimit(NONE)).hasValue(D30);
    assertThat(subject.searchLimit(CAR)).hasValue(D30);
    assertThat(subject.hasTransfersFrom(NONE)).isTrue();
    assertThat(subject.accepts(MIN_30, NONE, NONE)).isTrue();
    assertThat(subject.accepts(MIN_30 + 1, CAR, CAR)).isFalse();
  }

  @Test
  void carHasTransfersBetweenStopsThatAllowCarsOnly() {
    var subject = new CarMaxDuration(Duration.ofHours(3));
    assertThat(subject.maxDuration()).isEqualTo(Duration.ofHours(3));
    assertThat(subject.searchLimit(CAR)).hasValue(Duration.ofHours(3));
    assertThat(subject.searchLimit(BICYCLE_AND_CAR)).hasValue(Duration.ofHours(3));
    assertThat(subject.hasTransfersFrom(CAR)).isTrue();

    assertThat(subject.searchLimit(NONE)).isEmpty();
    assertThat(subject.searchLimit(BICYCLE)).isEmpty();
    assertThat(subject.hasTransfersFrom(NONE)).isFalse();

    assertThat(subject.accepts(60, CAR, CAR)).isTrue();
    assertThat(subject.accepts(3 * 3600, CAR, CAR)).isTrue();
    assertThat(subject.accepts(3 * 3600 + 1, CAR, CAR)).isFalse();
    assertThat(subject.accepts(60, CAR, NONE)).isFalse();
    assertThat(subject.accepts(60, NONE, CAR)).isFalse();
    assertThat(subject.accepts(60, BICYCLE, BICYCLE)).isFalse();
  }

  @Test
  void bicycleLongestDurationIsTheLongestOfTheLimits() {
    assertThat(BICYCLE_ALL.longestDuration()).isEqualTo(D60);
    assertThat(new BicycleMaxDurations(D20, D45, null).longestDuration()).isEqualTo(D45);
    assertThat(new BicycleMaxDurations(D20, null, null).longestDuration()).isEqualTo(D20);
  }

  @Test
  void bicycleSearchLimitIsTheLongestLimitThatAppliesToTheModesOfTheStop() {
    assertThat(BICYCLE_ALL.searchLimit(NONE)).hasValue(D20);
    assertThat(BICYCLE_ALL.searchLimit(BICYCLE)).hasValue(D45);
    assertThat(BICYCLE_ALL.searchLimit(CAR)).hasValue(D60);
    assertThat(BICYCLE_ALL.searchLimit(BICYCLE_AND_CAR)).hasValue(D60);
    assertThat(BICYCLE_ALL.hasTransfersFrom(NONE)).isTrue();
  }

  @Test
  void bicycleAcceptsTheAllStopsLimitBetweenAnyStops() {
    assertThat(BICYCLE_ALL.accepts(20 * 60, NONE, NONE)).isTrue();
    assertThat(BICYCLE_ALL.accepts(20 * 60 + 1, NONE, NONE)).isFalse();
    assertThat(BICYCLE_ALL.accepts(20 * 60 + 1, BICYCLE, NONE)).isFalse();
  }

  @Test
  void bicycleAcceptsALongerTransferBetweenStopsThatAllowBicycles() {
    assertThat(BICYCLE_ALL.accepts(MIN_40, BICYCLE, BICYCLE)).isTrue();
    assertThat(BICYCLE_ALL.accepts(MIN_45, BICYCLE, BICYCLE)).isTrue();
    assertThat(BICYCLE_ALL.accepts(MIN_45 + 1, BICYCLE, BICYCLE)).isFalse();
    assertThat(BICYCLE_ALL.accepts(MIN_40, BICYCLE, NONE)).isFalse();
    assertThat(BICYCLE_ALL.accepts(MIN_40, NONE, BICYCLE)).isFalse();
    assertThat(BICYCLE_ALL.accepts(MIN_40, BICYCLE, CAR)).isFalse();
    assertThat(BICYCLE_ALL.accepts(MIN_40, BICYCLE, BICYCLE_AND_CAR)).isTrue();
  }

  @Test
  void bicycleAcceptsALongerTransferBetweenStopsThatAllowCars() {
    assertThat(BICYCLE_ALL.accepts(MIN_60, CAR, CAR)).isTrue();
    assertThat(BICYCLE_ALL.accepts(MIN_60 + 1, CAR, CAR)).isFalse();
    assertThat(BICYCLE_ALL.accepts(MIN_60, CAR, NONE)).isFalse();
    assertThat(BICYCLE_ALL.accepts(MIN_60, NONE, CAR)).isFalse();
    assertThat(BICYCLE_ALL.accepts(MIN_60, CAR, BICYCLE)).isFalse();
    assertThat(BICYCLE_ALL.accepts(MIN_60, BICYCLE_AND_CAR, BICYCLE_AND_CAR)).isTrue();
  }

  @Test
  void theBicycleAndCarLimitsAreIndependent() {
    var carOnly = new BicycleMaxDurations(D20, null, D60);
    assertThat(carOnly.accepts(MIN_40, BICYCLE, BICYCLE)).isFalse();
    assertThat(carOnly.accepts(MIN_40, CAR, CAR)).isTrue();
    var bicycleOnly = new BicycleMaxDurations(D20, D45, null);
    assertThat(bicycleOnly.accepts(MIN_40, CAR, CAR)).isFalse();
    assertThat(bicycleOnly.accepts(MIN_40, BICYCLE, BICYCLE)).isTrue();
  }

  @Test
  void bicycleWithoutTheAllStopsLimitHasTransfersBetweenAllowedStopsOnly() {
    var subject = new BicycleMaxDurations(null, D45, D60);

    assertThat(subject.searchLimit(NONE)).isEmpty();
    assertThat(subject.hasTransfersFrom(NONE)).isFalse();
    assertThat(subject.searchLimit(BICYCLE)).hasValue(D45);
    assertThat(subject.searchLimit(CAR)).hasValue(D60);
    assertThat(subject.accepts(60, NONE, NONE)).isFalse();
    assertThat(subject.accepts(60, BICYCLE, BICYCLE)).isTrue();
    assertThat(subject.accepts(60, BICYCLE, NONE)).isFalse();
  }

  @Test
  void bicycleNeedsAtLeastOneLimit() {
    assertThrows(IllegalArgumentException.class, () -> new BicycleMaxDurations(null, null, null));
  }

  @Test
  void theLimitsOfTheAllowedStopsMustBeLongerThanTheAllStopsLimit() {
    assertThrows(IllegalArgumentException.class, () -> new BicycleMaxDurations(D30, D30, null));
    assertThrows(IllegalArgumentException.class, () -> new BicycleMaxDurations(D45, D30, null));
    assertThrows(IllegalArgumentException.class, () -> new BicycleMaxDurations(D30, null, D30));
    assertThrows(IllegalArgumentException.class, () -> new BicycleMaxDurations(D45, null, D30));
    // The limits of the allowed stops have no relation to each other
    assertThat(new BicycleMaxDurations(D20, D60, D45).maxDurationAllowsCar()).isEqualTo(D45);
    assertThat(new BicycleMaxDurations(D20, D45, D60).maxDuration()).isEqualTo(D45);
  }

  @Test
  void bicycleLimitsMustBePositive() {
    assertThrows(IllegalArgumentException.class, () ->
      new BicycleMaxDurations(null, Duration.ZERO, null)
    );
    assertThrows(IllegalArgumentException.class, () ->
      new BicycleMaxDurations(null, null, Duration.ofMinutes(-1))
    );
  }
}
