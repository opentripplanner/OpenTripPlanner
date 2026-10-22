package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.raptor.data.stop.RaptorStopIndex;
import org.opentripplanner.transit.transfer.regular.FakeTransferPathProvider;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.api.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfile;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfiles;

class DefaultTransferGeneratorTest {

  private static final FeedScopedId A = FeedScopedId.of("F", "A");
  private static final FeedScopedId B = FeedScopedId.of("F", "B");
  private static final FeedScopedId C = FeedScopedId.of("F", "C");

  private static RaptorStopIndex stopIndex() {
    return TestStopIndex.of(A, B, C);
  }

  private static WalkPreferences withReluctance(double reluctance) {
    return WalkPreferences.of().withReluctance(Reluctance.of(reluctance)).build();
  }

  @Test
  void keepsOnlyTheCheapestCandidatePerTargetStop() {
    var provider = new FakeTransferPathProvider();
    var cheapAB = provider.addNearby(A, B, 100, 60);
    provider.addNearby(A, B, 200, 90);

    AbstractUserPreferences<?> userPreferences = withReluctance(1.0);
    // Explicit type witnesses throughout: diamond inference from a wildcard-typed argument
    // triggers capture conversion and produces a fresh, non-matching captured type at each use.
    TransferProfile<AbstractUserPreferences<?>> profile = new TransferProfile<
      AbstractUserPreferences<?>
    >(userPreferences, userPreferences);
    TransferProfiles<AbstractUserPreferences<?>> profiles = TransferProfiles.<
      AbstractUserPreferences<?>
    >of(List.of(profile));

    var stopIndex = stopIndex();
    var buildRepository =
      new DefaultRegularTransferBuildRepository<FakeTransferPathProvider.FakePath>();
    var generator = new DefaultTransferGenerator<
      FakeTransferPathProvider.FakePath,
      AbstractUserPreferences<?>
    >(stopIndex, List.of(A), provider, profiles, buildRepository);

    generator.generateTransfersForAllStops();

    assertThat(
      TestLifecycles.initialSnapshot(buildRepository).findPath(
        userPreferences,
        stopIndex.toStopIndex(A),
        stopIndex.toStopIndex(B)
      )
    ).isSameInstanceAs(cheapAB);
  }

  @Test
  void pathsExceedingTheProviderConfiguredDurationLimitAreExcluded() {
    var provider = new FakeTransferPathProvider().withMaxDurationSeconds(120);
    provider.addNearby(A, B, 100, 600);

    AbstractUserPreferences<?> userPreferences = withReluctance(1.0);
    TransferProfile<AbstractUserPreferences<?>> profile = new TransferProfile<
      AbstractUserPreferences<?>
    >(userPreferences, userPreferences);
    TransferProfiles<AbstractUserPreferences<?>> profiles = TransferProfiles.<
      AbstractUserPreferences<?>
    >of(List.of(profile));

    var stopIndex = stopIndex();
    var buildRepository =
      new DefaultRegularTransferBuildRepository<FakeTransferPathProvider.FakePath>();
    var generator = new DefaultTransferGenerator<
      FakeTransferPathProvider.FakePath,
      AbstractUserPreferences<?>
    >(stopIndex, List.of(A), provider, profiles, buildRepository);

    generator.generateTransfersForAllStops();

    assertThat(
      TestLifecycles.initialSnapshot(buildRepository).findPath(
        userPreferences,
        stopIndex.toStopIndex(A),
        stopIndex.toStopIndex(B)
      )
    ).isNull();
  }

  @Test
  void candidatesOutsideTheStopIndexAreSkipped() {
    var flexStop = FeedScopedId.of("F", "FLEX");
    var provider = new FakeTransferPathProvider();
    var pathAB = provider.addNearby(A, B, 100, 60);
    provider.addNearby(A, flexStop, 50, 30);

    AbstractUserPreferences<?> userPreferences = withReluctance(1.0);
    TransferProfile<AbstractUserPreferences<?>> profile = new TransferProfile<
      AbstractUserPreferences<?>
    >(userPreferences, userPreferences);
    TransferProfiles<AbstractUserPreferences<?>> profiles = TransferProfiles.<
      AbstractUserPreferences<?>
    >of(List.of(profile));

    var buildRepository =
      new DefaultRegularTransferBuildRepository<FakeTransferPathProvider.FakePath>();
    var generator = new DefaultTransferGenerator<
      FakeTransferPathProvider.FakePath,
      AbstractUserPreferences<?>
    >(stopIndex(), List.of(A), provider, profiles, buildRepository);

    generator.generateTransfersForAllStops();

    assertThat(buildRepository.calculateNumberOfTransferPaths(userPreferences)).isEqualTo(1);
    assertThat(
      TestLifecycles.initialSnapshot(buildRepository).findPath(userPreferences, 0, 1)
    ).isEqualTo(pathAB);
  }
}
