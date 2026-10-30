package org.opentripplanner.transit.transfer.regular.api.parameters;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.basic.Speed;
import org.opentripplanner.transit.transfer.regular.api.maxduration.BicycleMaxDurations;
import org.opentripplanner.transit.transfer.regular.api.maxduration.CarMaxDuration;
import org.opentripplanner.transit.transfer.regular.api.maxduration.MaxDurationsPreference;
import org.opentripplanner.transit.transfer.regular.api.preferences.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WheelchairPreferences;

class TransferParametersTest {

  private static final WalkPreferences WALK = WalkPreferences.DEFAULT;
  private static final WalkPreferences OTHER_WALK = WalkPreferences.of()
    .withReluctance(Reluctance.of(5.0))
    .build();

  @Test
  void keepsTheProfilesInTheDeclaredOrder() {
    var first = new TransferProfile<>("first", TransferProfileType.WALK, OTHER_WALK, "first");
    var second = new TransferProfile<>("second", TransferProfileType.WALK, WALK, "second");

    var subject = new TransferParameters<>(List.of(first, second));

    assertThat(subject.profiles()).containsExactly(first, second).inOrder();
  }

  @Test
  void requiresAtLeastOneProfile() {
    assertThrows(IllegalArgumentException.class, () -> new TransferParameters<String>(List.of()));
  }

  @Test
  void rejectsProfilesWithTheSameId() {
    var profiles = List.of(
      new TransferProfile<>("walk", TransferProfileType.WALK, WALK, "first"),
      new TransferProfile<>("walk", TransferProfileType.WALK, OTHER_WALK, "second")
    );

    var ex = assertThrows(IllegalArgumentException.class, () -> new TransferParameters<>(profiles));
    assertThat(ex).hasMessageThat().contains("walk");
  }

  @Test
  void aProfileNeedsAnId() {
    assertThrows(IllegalArgumentException.class, () ->
      new TransferProfile<>(" ", TransferProfileType.WALK, WALK, "request")
    );
    assertThrows(NullPointerException.class, () ->
      new TransferProfile<>(null, TransferProfileType.WALK, WALK, "request")
    );
  }

  @Test
  void anyNonBlankIdIsAccepted() {
    var profile = new TransferProfile<>(
      "Walk: 1.3 m/s (åre) #1",
      TransferProfileType.WALK,
      WALK,
      "request"
    );

    assertThat(profile.id()).isEqualTo("Walk: 1.3 m/s (åre) #1");
  }

  @Test
  void aProfileMayHaveNoUserPreferences() {
    var profile = new TransferProfile<>("walk", TransferProfileType.WALK, null, "request");

    assertThat(profile.userPreferences()).isNull();
  }

  @Test
  void rejectsAProfileWithoutARoutingRequest() {
    assertThrows(NullPointerException.class, () ->
      new TransferProfile<String>("walk", TransferProfileType.WALK, WALK, null)
    );
  }

  @Test
  void theMaxDurationsOfATypeAreTheOnesConfigured() {
    var profiles = List.of(new TransferProfile<>("walk", TransferProfileType.WALK, WALK, "walk"));

    var subject = new TransferParameters<>(
      profiles,
      Map.of(TransferProfileType.WALK, MaxDurationsPreference.of(Duration.ofMinutes(45)))
    );

    assertThat(subject.maxDurations(TransferProfileType.WALK).longestDuration()).isEqualTo(
      Duration.ofMinutes(45)
    );
  }

  @Test
  void aTypeWithProfilesNeedsMaxDurations() {
    var profiles = List.of(
      new TransferProfile<>("walk", TransferProfileType.WALK, WALK, "walk"),
      new TransferProfile<>(
        "wheelchair",
        TransferProfileType.WHEELCHAIR,
        WheelchairPreferences.DEFAULT,
        "wheelchair"
      )
    );

    var ex = assertThrows(IllegalArgumentException.class, () ->
      new TransferParameters<>(
        profiles,
        Map.of(TransferProfileType.WALK, MaxDurationsPreference.DEFAULT)
      )
    );

    assertThat(ex).hasMessageThat().contains("wheelchair");
  }

  @Test
  void theMaxDurationsOfATypeWithoutProfilesAreNotRequired() {
    var profiles = List.of(new TransferProfile<>("walk", TransferProfileType.WALK, WALK, "walk"));

    var subject = new TransferParameters<>(
      profiles,
      Map.of(
        TransferProfileType.WALK,
        MaxDurationsPreference.DEFAULT,
        TransferProfileType.CAR,
        new CarMaxDuration(Duration.ofHours(3))
      )
    );

    assertThrows(IllegalArgumentException.class, () ->
      subject.maxDurations(TransferProfileType.BICYCLE)
    );
  }

  @Test
  void theMaxDurationsOfATypeMustBeOfTheKindOfTheType() {
    var profiles = List.of(new TransferProfile<>("walk", TransferProfileType.WALK, WALK, "w"));
    MaxDurationsPreference plain = MaxDurationsPreference.of(Duration.ofMinutes(20));
    MaxDurationsPreference bicycle = new BicycleMaxDurations(null, Duration.ofMinutes(20), null);
    MaxDurationsPreference car = new CarMaxDuration(Duration.ofMinutes(20));
    var walk = Map.of(TransferProfileType.WALK, plain);

    assertThrows(IllegalArgumentException.class, () ->
      new TransferParameters<>(
        profiles,
        Map.of(TransferProfileType.WALK, plain, TransferProfileType.CAR, plain)
      )
    );
    assertThrows(IllegalArgumentException.class, () ->
      new TransferParameters<>(profiles, Map.of(TransferProfileType.WALK, bicycle))
    );
    assertThrows(IllegalArgumentException.class, () ->
      new TransferParameters<>(
        profiles,
        Map.of(TransferProfileType.WALK, plain, TransferProfileType.BICYCLE, car)
      )
    );
    var subject = new TransferParameters<>(
      profiles,
      Map.of(
        TransferProfileType.WALK,
        plain,
        TransferProfileType.BICYCLE,
        bicycle,
        TransferProfileType.CAR,
        car
      )
    );
    assertThat(subject.maxDurations(TransferProfileType.BICYCLE)).isEqualTo(bicycle);
    assertThat(subject.maxDurations(TransferProfileType.CAR)).isEqualTo(car);
    assertThat(
      new TransferParameters<>(profiles, walk).maxDurations(TransferProfileType.WALK)
    ).isEqualTo(plain);
  }

  @Test
  void theImplicitConfigurationHasThirtyMinutesForEachTypeOfTheProfiles() {
    var subject = new TransferParameters<>(
      List.of(new TransferProfile<>("walk", TransferProfileType.WALK, WALK, "w"))
    );

    assertThat(subject.maxDurations(TransferProfileType.WALK)).isEqualTo(
      MaxDurationsPreference.DEFAULT
    );
    assertThat(MaxDurationsPreference.DEFAULT.longestDuration()).isEqualTo(Duration.ofMinutes(30));
  }

  @Test
  void aParentThatDoesNotExistIsRejectedAndNamed() {
    var profiles = List.of(
      new TransferProfile<>("walk", TransferProfileType.WALK, null, WALK, "walk"),
      new TransferProfile<>("slow", TransferProfileType.WALK, "missing", OTHER_WALK, "slow")
    );

    var ex = assertThrows(IllegalArgumentException.class, () -> new TransferParameters<>(profiles));
    assertThat(ex).hasMessageThat().contains("'missing'");
    assertThat(ex).hasMessageThat().contains("'slow'");
  }

  @Test
  void aCycleOfParentsIsRejected() {
    var profiles = List.of(
      new TransferProfile<>("a", TransferProfileType.WALK, "b", WALK, "a"),
      new TransferProfile<>("b", TransferProfileType.WALK, "a", OTHER_WALK, "b")
    );

    var ex = assertThrows(IllegalArgumentException.class, () -> new TransferParameters<>(profiles));
    assertThat(ex).hasMessageThat().contains("cycle");
  }

  @Test
  void aProfileCanNotBeItsOwnParent() {
    var profiles = List.of(new TransferProfile<>("a", TransferProfileType.WALK, "a", WALK, "a"));

    assertThrows(IllegalArgumentException.class, () -> new TransferParameters<>(profiles));
  }

  @Test
  void aBlankParentIsRejected() {
    assertThrows(IllegalArgumentException.class, () ->
      new TransferProfile<>("a", TransferProfileType.WALK, " ", WALK, "a")
    );
  }

  private static TransferParameters<String> withWarmup(WarmupParameters... warmup) {
    return new TransferParameters<>(
      List.of(new TransferProfile<>("walk", TransferProfileType.WALK, WALK, "walk")),
      Map.of(TransferProfileType.WALK, MaxDurationsPreference.DEFAULT),
      List.of(warmup)
    );
  }

  @Test
  void keepsTheWarmupEntries() {
    var entry = new WarmupParameters("avoid", "walk", OTHER_WALK);

    assertThat(withWarmup(entry).warmup()).containsExactly(entry);
  }

  @Test
  void theParentOfAWarmupEntryMustBeAProfile() {
    var ex = assertThrows(IllegalArgumentException.class, () ->
      withWarmup(new WarmupParameters("avoid", "missing", OTHER_WALK))
    );
    assertThat(ex).hasMessageThat().contains("'missing'");
    assertThat(ex).hasMessageThat().contains("'avoid'");
  }

  @Test
  void aWarmupEntryCanNotBeTheParentOfAnotherWarmupEntry() {
    assertThrows(IllegalArgumentException.class, () ->
      withWarmup(
        new WarmupParameters("a", "walk", OTHER_WALK),
        new WarmupParameters(
          "b",
          "a",
          WalkPreferences.of().withReluctance(Reluctance.of(7.0)).build()
        )
      )
    );
  }

  @Test
  void aWarmupIdMustBeUniqueAmongTheProfilesAndTheWarmupEntries() {
    var ex = assertThrows(IllegalArgumentException.class, () ->
      withWarmup(new WarmupParameters("walk", "walk", OTHER_WALK))
    );
    assertThat(ex).hasMessageThat().contains("walk");
    assertThrows(IllegalArgumentException.class, () ->
      withWarmup(
        new WarmupParameters("a", "walk", OTHER_WALK),
        new WarmupParameters(
          "a",
          "walk",
          WalkPreferences.of().withSpeed(Speed.ofMetersPerSecond(1.0)).build()
        )
      )
    );
  }

  @Test
  void aWarmupEntryNeedsAnIdAParentAndPreferences() {
    assertThrows(IllegalArgumentException.class, () -> new WarmupParameters(" ", "walk", WALK));
    assertThrows(IllegalArgumentException.class, () -> new WarmupParameters("a", " ", WALK));
    assertThrows(NullPointerException.class, () -> new WarmupParameters("a", "walk", null));
  }

  @Test
  void theDefaultReuseLimitIsFifteenSecondsAndTwoPercent() {
    var limit = TransferParameters.DEFAULT_REUSE_LIMIT;

    assertThat(limit.calculate(Cost.ZERO).toSeconds()).isEqualTo(15);
    // 15 seconds + 2% of 1000 seconds
    assertThat(limit.calculate(Cost.costOfSeconds(1000)).toSeconds()).isEqualTo(35);
  }
}
