package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.raptor.data.stop.StopIndex;
import org.opentripplanner.transit.transfer.regular.FakeTransferPathProvider;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.api.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfile;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfileType;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfiles;

class DefaultTransferGeneratorTest {

  private static final FeedScopedId A = FeedScopedId.of("F", "A");
  private static final FeedScopedId B = FeedScopedId.of("F", "B");
  private static final FeedScopedId C = FeedScopedId.of("F", "C");

  private record TestStop(FeedScopedId id, int index) {}

  private static StopIndex stopIndex() {
    var stops = List.of(new TestStop(A, 0), new TestStop(B, 1), new TestStop(C, 2));
    return new StopIndex(stops.size(), stops, TestStop::index, TestStop::id);
  }

  private static WalkPreferences withReluctance(double reluctance) {
    return WalkPreferences.of().withReluctance(Reluctance.of(reluctance)).build();
  }

  @Test
  void keepsOnlyTheCheapestCandidatePerTargetStop() {
    var provider = new FakeTransferPathProvider();
    var cheapAB = provider.addNearby(TransferProfileType.WALK, A, B, 100, 60);
    provider.addNearby(TransferProfileType.WALK, A, B, 200, 90);

    AbstractUserPreferences<?> preferences = withReluctance(1.0);
    // Explicit type witnesses throughout: diamond inference from a wildcard-typed argument
    // triggers capture conversion and produces a fresh, non-matching captured type at each use.
    TransferProfile<AbstractUserPreferences<?>> profile = new TransferProfile<
      AbstractUserPreferences<?>
    >(TransferProfileType.WALK, preferences, preferences);
    TransferProfiles<AbstractUserPreferences<?>> profiles = TransferProfiles.<
      AbstractUserPreferences<?>
    >of(List.of(profile));

    var stopIndex = stopIndex();
    var repository = new DefaultRegularTransferRepository<FakeTransferPathProvider.FakePath>();
    var generator = new DefaultTransferGenerator<
      FakeTransferPathProvider.FakePath,
      AbstractUserPreferences<?>
    >(stopIndex, List.of(A), provider, profiles, repository);

    generator.generateTransfersForAllStops();

    assertThat(
      repository
        .freeze()
        .findPath(TransferProfileType.WALK, stopIndex.toStopIndex(A), stopIndex.toStopIndex(B))
    ).isSameInstanceAs(cheapAB);
  }

  @Test
  void pathsExceedingTheProviderConfiguredDurationLimitAreExcluded() {
    var provider = new FakeTransferPathProvider().withMaxDurationSeconds(120);
    provider.addNearby(TransferProfileType.WALK, A, B, 100, 600);

    AbstractUserPreferences<?> preferences = withReluctance(1.0);
    TransferProfile<AbstractUserPreferences<?>> profile = new TransferProfile<
      AbstractUserPreferences<?>
    >(TransferProfileType.WALK, preferences, preferences);
    TransferProfiles<AbstractUserPreferences<?>> profiles = TransferProfiles.<
      AbstractUserPreferences<?>
    >of(List.of(profile));

    var stopIndex = stopIndex();
    var repository = new DefaultRegularTransferRepository<FakeTransferPathProvider.FakePath>();
    var generator = new DefaultTransferGenerator<
      FakeTransferPathProvider.FakePath,
      AbstractUserPreferences<?>
    >(stopIndex, List.of(A), provider, profiles, repository);

    generator.generateTransfersForAllStops();

    assertThat(
      repository
        .freeze()
        .findPath(TransferProfileType.WALK, stopIndex.toStopIndex(A), stopIndex.toStopIndex(B))
    ).isNull();
  }
}
