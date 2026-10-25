package org.opentripplanner.transit.transfer.regular.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.basic.Speed;

class TransferProfilesParametersTest {

  private static final WalkPreferences WALK = WalkPreferences.DEFAULT;
  private static final WalkPreferences OTHER_WALK = WalkPreferences.of()
    .withReluctance(Reluctance.of(5.0))
    .build();

  @Test
  void keepsTheProfilesInTheDeclaredOrder() {
    var first = new WalkProfileParameters<>("first", OTHER_WALK, "first");
    var second = new WalkProfileParameters<>("second", WALK, "second");

    var subject = new TransferProfilesParameters<>(List.of(first, second));

    assertThat(subject.walk()).containsExactly(first, second).inOrder();
  }

  @Test
  void requiresAtLeastOneProfile() {
    assertThrows(IllegalArgumentException.class, () ->
      new TransferProfilesParameters<String>(List.of())
    );
  }

  @Test
  void rejectsProfilesWithTheSameId() {
    var profiles = List.of(
      new WalkProfileParameters<>("walk", WALK, "first"),
      new WalkProfileParameters<>("walk", OTHER_WALK, "second")
    );

    var ex = assertThrows(IllegalArgumentException.class, () ->
      new TransferProfilesParameters<>(profiles)
    );
    assertThat(ex).hasMessageThat().contains("walk");
  }

  @Test
  void aProfileNeedsAnId() {
    assertThrows(IllegalArgumentException.class, () ->
      new WalkProfileParameters<>(" ", WALK, "request")
    );
    assertThrows(NullPointerException.class, () ->
      new WalkProfileParameters<>(null, WALK, "request")
    );
  }

  @Test
  void anyNonBlankIdIsAccepted() {
    var profile = new WalkProfileParameters<>("Walk: 1.3 m/s (åre) #1", WALK, "request");

    assertThat(profile.id()).isEqualTo("Walk: 1.3 m/s (åre) #1");
  }

  @Test
  void aProfileMayHaveNoUserPreferences() {
    var profile = new WalkProfileParameters<>("walk", null, "request");

    assertThat(profile.userPreferences()).isNull();
  }

  @Test
  void rejectsAProfileWithoutARoutingRequest() {
    assertThrows(NullPointerException.class, () ->
      new WalkProfileParameters<String>("walk", WALK, null)
    );
  }

  @Test
  void aTypeWithoutAnOverrideUsesTheDefaultMaxDurations() {
    var profiles = List.of(new WalkProfileParameters<>("walk", WALK, "walk"));

    var subject = new TransferProfilesParameters<>(
      profiles,
      MaxDurationsPreference.of(Duration.ofMinutes(20)),
      Map.of()
    );

    assertThat(subject.maxDurations(TransferProfileType.WALK).maxDuration()).isEqualTo(
      Duration.ofMinutes(20)
    );
  }

  @Test
  void anOverrideReplacesTheDefaultMaxDurationsOfTheType() {
    var profiles = List.of(new WalkProfileParameters<>("walk", WALK, "walk"));

    var subject = new TransferProfilesParameters<>(
      profiles,
      MaxDurationsPreference.of(Duration.ofMinutes(20)),
      Map.of(TransferProfileType.WALK, MaxDurationsPreference.of(Duration.ofMinutes(45)))
    );

    assertThat(subject.maxDurations(TransferProfileType.WALK).maxDuration()).isEqualTo(
      Duration.ofMinutes(45)
    );
  }

  @Test
  void theDefaultMaxDurationsAreThirtyMinutes() {
    var subject = new TransferProfilesParameters<>(
      List.of(new WalkProfileParameters<>("walk", WALK, "w"))
    );

    assertThat(subject.maxDurations(TransferProfileType.WALK)).isEqualTo(
      MaxDurationsPreference.DEFAULT
    );
    assertThat(MaxDurationsPreference.DEFAULT.maxDuration()).isEqualTo(Duration.ofMinutes(30));
  }

  @Test
  void aParentThatDoesNotExistIsRejectedAndNamed() {
    var profiles = List.of(
      new WalkProfileParameters<>("walk", null, WALK, "walk"),
      new WalkProfileParameters<>("slow", "missing", OTHER_WALK, "slow")
    );

    var ex = assertThrows(IllegalArgumentException.class, () ->
      new TransferProfilesParameters<>(profiles)
    );
    assertThat(ex).hasMessageThat().contains("'missing'");
    assertThat(ex).hasMessageThat().contains("'slow'");
  }

  @Test
  void aCycleOfParentsIsRejected() {
    var profiles = List.of(
      new WalkProfileParameters<>("a", "b", WALK, "a"),
      new WalkProfileParameters<>("b", "a", OTHER_WALK, "b")
    );

    var ex = assertThrows(IllegalArgumentException.class, () ->
      new TransferProfilesParameters<>(profiles)
    );
    assertThat(ex).hasMessageThat().contains("cycle");
  }

  @Test
  void aProfileCanNotBeItsOwnParent() {
    var profiles = List.of(new WalkProfileParameters<>("a", "a", WALK, "a"));

    assertThrows(IllegalArgumentException.class, () -> new TransferProfilesParameters<>(profiles));
  }

  @Test
  void aBlankParentIsRejected() {
    assertThrows(IllegalArgumentException.class, () ->
      new WalkProfileParameters<>("a", " ", WALK, "a")
    );
  }

  @Test
  void theDeduplicationLimitIsTheDefaultIfNotGiven() {
    var subject = new TransferProfilesParameters<>(
      List.of(new WalkProfileParameters<>("w", WALK, "w"))
    );

    assertThat(subject.deduplicationLimit()).isEqualTo(DeduplicationLimit.DEFAULT);
  }

  private static TransferProfilesParameters<String> withWarmup(WarmupParameters... warmup) {
    return new TransferProfilesParameters<>(
      List.of(new WalkProfileParameters<>("walk", WALK, "walk")),
      MaxDurationsPreference.DEFAULT,
      Map.of(),
      DeduplicationLimit.DEFAULT,
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
}
