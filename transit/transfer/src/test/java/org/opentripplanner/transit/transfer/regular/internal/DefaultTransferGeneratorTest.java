package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.transit.transfer.regular.FakeTransferPathProvider.FakePrefs;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.raptor.data.stop.StopIndex;
import org.opentripplanner.transit.transfer.regular.FakeTransferPathProvider;
import org.opentripplanner.transit.transfer.regular.RaptorTransferProfile;
import org.opentripplanner.transit.transfer.regular.spi.RegularTransferParameters;

class DefaultTransferGeneratorTest {

  private static final FeedScopedId A = FeedScopedId.of("F", "A");
  private static final FeedScopedId B = FeedScopedId.of("F", "B");
  private static final FeedScopedId C = FeedScopedId.of("F", "C");
  private static final FeedScopedId D = FeedScopedId.of("F", "D");

  private record TestStop(FeedScopedId id, int index) {}

  private static StopIndex stopIndex() {
    var stops = List.of(
      new TestStop(A, 0),
      new TestStop(B, 1),
      new TestStop(C, 2),
      new TestStop(D, 3)
    );
    return new StopIndex(stops.size(), stops, TestStop::index, TestStop::id);
  }

  /** 20% of the base cost, plus a flat 10 - mirrors a "0.2x + 10" linear tolerance. */
  private static int tolerance(int baseCost) {
    return (int) Math.round(baseCost * 0.2) + 10;
  }

  @Test
  void ownSearchStillRunsAndDeduplicatesAgainstBaseWhenCostEquivalent() {
    var provider = new FakeTransferPathProvider();
    var walkAB = provider.addNearby(RaptorTransferProfile.WALK, A, B, 100, 60);
    provider.addNearby(RaptorTransferProfile.WALK, A, C, 200, 90);
    provider.addNearby(RaptorTransferProfile.BICYCLE, A, B, 90, 40);
    var bikeAD = provider.addNearby(RaptorTransferProfile.BICYCLE, A, D, 30, 20);
    var bikeAC = provider.addNearby(RaptorTransferProfile.BICYCLE, A, C, 10, 50);

    var walkProfile = new RegularTransferParameters<>(
      RaptorTransferProfile.WALK,
      null,
      new FakePrefs(1.0, 120)
    );
    var bikeProfile = new RegularTransferParameters<>(
      RaptorTransferProfile.BICYCLE,
      RaptorTransferProfile.WALK,
      new FakePrefs(1.0, 120)
    );

    var stopIndex = stopIndex();
    var repository = new RegularTransferRepository<FakeTransferPathProvider.FakePath>();
    var generator = new DefaultTransferGenerator<>(
      stopIndex,
      List.of(A),
      provider,
      List.of(walkProfile, bikeProfile),
      DefaultTransferGeneratorTest::tolerance,
      repository
    );

    generator.generateTransfersForAllStops();

    int a = stopIndex.toStopIndex(A);
    int b = stopIndex.toStopIndex(B);
    int c = stopIndex.toStopIndex(C);
    int d = stopIndex.toStopIndex(D);

    // Walk: both of its own discovered paths are stored as-is.
    assertThat(repository.findPath(RaptorTransferProfile.WALK, a, b)).isSameInstanceAs(walkAB);
    assertThat(repository.findPath(RaptorTransferProfile.WALK, a, d)).isNull();

    // Bicycle A->B: own cost (90) is close enough to walk's path re-costed under bike's own
    // preferences (100) - deduplicated, so bicycle shares walk's exact path reference.
    assertThat(repository.findPath(RaptorTransferProfile.BICYCLE, a, b)).isSameInstanceAs(walkAB);

    // Bicycle A->D: walk never found this stop, so there is nothing to deduplicate against -
    // bicycle's own discovered path is kept.
    assertThat(repository.findPath(RaptorTransferProfile.BICYCLE, a, d)).isSameInstanceAs(bikeAD);

    // Bicycle A->C: own cost (10) is far cheaper than walk's path re-costed under bike's own
    // preferences (200) - not within tolerance, so bicycle keeps its own, distinct path.
    assertThat(repository.findPath(RaptorTransferProfile.BICYCLE, a, c)).isSameInstanceAs(bikeAC);
  }

  @Test
  void pathsExceedingTheProfilesOwnDurationLimitAreExcluded() {
    var provider = new FakeTransferPathProvider();
    provider.addNearby(RaptorTransferProfile.WALK, A, B, 100, 600);

    var walkProfile = new RegularTransferParameters<>(
      RaptorTransferProfile.WALK,
      null,
      new FakePrefs(1.0, 120)
    );

    var stopIndex = stopIndex();
    var repository = new RegularTransferRepository<FakeTransferPathProvider.FakePath>();
    var generator = new DefaultTransferGenerator<>(
      stopIndex,
      List.of(A),
      provider,
      List.of(walkProfile),
      DefaultTransferGeneratorTest::tolerance,
      repository
    );

    generator.generateTransfersForAllStops();

    assertThat(
      repository.findPath(
        RaptorTransferProfile.WALK,
        stopIndex.toStopIndex(A),
        stopIndex.toStopIndex(B)
      )
    ).isNull();
  }
}
