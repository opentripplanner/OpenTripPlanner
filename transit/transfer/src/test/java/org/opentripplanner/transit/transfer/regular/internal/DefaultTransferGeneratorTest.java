package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.raptor.data.stop.RaptorStopIndex;
import org.opentripplanner.transit.transfer.regular.FakeRequestMapper;
import org.opentripplanner.transit.transfer.regular.FakeTransferPathProvider;
import org.opentripplanner.transit.transfer.regular.api.TransferProfilesParameters;
import org.opentripplanner.transit.transfer.regular.api.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.api.WalkProfileParameters;

class DefaultTransferGeneratorTest {

  private static final FeedScopedId A = FeedScopedId.of("F", "A");
  private static final FeedScopedId B = FeedScopedId.of("F", "B");
  private static final FeedScopedId C = FeedScopedId.of("F", "C");

  private static RaptorStopIndex stopIndex() {
    return TestStopIndex.of(A, B, C);
  }

  private static TransferProfilesParameters<WalkPreferences> parameters(WalkPreferences walk) {
    return new TransferProfilesParameters<>(List.of(new WalkProfileParameters<>(walk, walk)));
  }

  private static WalkPreferences withReluctance(double reluctance) {
    return WalkPreferences.of().withReluctance(Reluctance.of(reluctance)).build();
  }

  @Test
  void keepsOnlyTheCheapestCandidatePerTargetStop() {
    var provider = new FakeTransferPathProvider();
    var cheapAB = provider.addNearby(A, B, 100, 60);
    provider.addNearby(A, B, 200, 90);

    WalkPreferences userPreferences = withReluctance(1.0);
    var stopIndex = stopIndex();
    var buildRepository =
      new DefaultRegularTransferBuildRepository<FakeTransferPathProvider.FakePath>();
    var generator = new DefaultTransferGenerator<>(
      stopIndex,
      parameters(userPreferences),
      provider,
      FakeRequestMapper.INSTANCE,
      buildRepository
    );

    generator.generateTransfersForAllStops();

    assertThat(
      TestLifecycles.initialSnapshot(buildRepository)
        .forRequest(userPreferences)
        .findPath(stopIndex.toStopIndex(A), stopIndex.toStopIndex(B))
    ).isSameInstanceAs(cheapAB);
  }

  @Test
  void pathsExceedingTheProviderConfiguredDurationLimitAreExcluded() {
    var provider = new FakeTransferPathProvider().withMaxDurationSeconds(120);
    provider.addNearby(A, B, 100, 600);

    WalkPreferences userPreferences = withReluctance(1.0);
    var stopIndex = stopIndex();
    var buildRepository =
      new DefaultRegularTransferBuildRepository<FakeTransferPathProvider.FakePath>();
    var generator = new DefaultTransferGenerator<>(
      stopIndex,
      parameters(userPreferences),
      provider,
      FakeRequestMapper.INSTANCE,
      buildRepository
    );

    generator.generateTransfersForAllStops();

    assertThat(
      TestLifecycles.initialSnapshot(buildRepository)
        .forRequest(userPreferences)
        .findPath(stopIndex.toStopIndex(A), stopIndex.toStopIndex(B))
    ).isNull();
  }

  @Test
  void candidatesOutsideTheStopIndexAreSkipped() {
    var flexStop = FeedScopedId.of("F", "FLEX");
    var provider = new FakeTransferPathProvider();
    var pathAB = provider.addNearby(A, B, 100, 60);
    provider.addNearby(A, flexStop, 50, 30);

    WalkPreferences userPreferences = withReluctance(1.0);
    var buildRepository =
      new DefaultRegularTransferBuildRepository<FakeTransferPathProvider.FakePath>();
    var generator = new DefaultTransferGenerator<>(
      stopIndex(),
      parameters(userPreferences),
      provider,
      FakeRequestMapper.INSTANCE,
      buildRepository
    );

    generator.generateTransfersForAllStops();

    assertThat(buildRepository.calculateNumberOfTransferPaths(userPreferences)).isEqualTo(1);
    assertThat(
      TestLifecycles.initialSnapshot(buildRepository).forRequest(userPreferences).findPath(0, 1)
    ).isEqualTo(pathAB);
  }

  @Test
  void listsTheStopsAtBothEndsOfTransfers() {
    var provider = new FakeTransferPathProvider();
    provider.addNearby(A, B, 100, 60);

    WalkPreferences userPreferences = withReluctance(1.0);
    var buildRepository =
      new DefaultRegularTransferBuildRepository<FakeTransferPathProvider.FakePath>();
    var generator = new DefaultTransferGenerator<>(
      stopIndex(),
      parameters(userPreferences),
      provider,
      FakeRequestMapper.INSTANCE,
      buildRepository
    );

    generator.generateTransfersForAllStops();

    assertThat(generator.listAllStopsUsedInTransfers()).containsExactly(A, B);
  }
}
