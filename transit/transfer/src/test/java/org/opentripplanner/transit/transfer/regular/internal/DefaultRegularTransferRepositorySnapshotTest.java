package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.raptor.data.stop.StopIndex;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.transit.transfer.regular.FakeTransferPathProvider;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.api.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfile;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfiles;

class DefaultRegularTransferRepositorySnapshotTest {

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

  /** Populates a repository the same way graph build would, via the real generator. */
  private static RegularTransferRepositorySnapshot<FakeTransferPathProvider.FakePath> generate(
    StopIndex stopIndex,
    FakeTransferPathProvider provider,
    double buildTimeReluctance
  ) {
    var buildRepository =
      new DefaultRegularTransferBuildRepository<FakeTransferPathProvider.FakePath>();
    AbstractUserPreferences<?> userPreferences = withReluctance(buildTimeReluctance);
    // configuredRequestParameters (R) is unread by DefaultTransferGenerator today - reuse the
    // preferences instance as a type-compatible placeholder. Explicit type witnesses throughout,
    // since diamond inference from a wildcard-typed argument triggers capture conversion and
    // produces a fresh, non-matching captured type at each use.
    TransferProfile<AbstractUserPreferences<?>> profile = new TransferProfile<
      AbstractUserPreferences<?>
    >(userPreferences, userPreferences);
    TransferProfiles<AbstractUserPreferences<?>> profiles = TransferProfiles.<
      AbstractUserPreferences<?>
    >of(List.of(profile));
    new DefaultTransferGenerator<FakeTransferPathProvider.FakePath, AbstractUserPreferences<?>>(
      stopIndex,
      List.of(A),
      provider,
      profiles,
      buildRepository
    ).generateTransfersForAllStops();
    return new RegularTransferRepositoryLifecycle<>(stopIndex, provider).freeze(
      buildRepository.createRuntimeRepository()
    );
  }

  /**
   * {@code RaptorTransfer} iterators here are flyweights (the same mutable object is returned by
   * every {@code next()}, per {@code @Flyweight}) - values must be snapshotted during iteration,
   * never deferred via a stream/list of the returned {@code RaptorTransfer} references.
   */
  private static List<Integer> collectC1(Iterator<? extends RaptorTransfer> it) {
    List<Integer> result = new ArrayList<>();
    while (it.hasNext()) {
      result.add(it.next().c1());
    }
    return result;
  }

  @Test
  void raptorTransfersAreRecostedUnderTheRequestPreferences() {
    var provider = new FakeTransferPathProvider();
    provider.addNearby(A, B, 100, 60);
    provider.addNearby(A, C, 200, 90);
    var stopIndex = stopIndex();
    var snapshot = generate(stopIndex, provider, 1.0);

    var transfers = snapshot.raptorTransfers(withReluctance(2.0));

    // Cost was re-costed under the request's own reluctance (2.0), not the build-time one.
    assertThat(collectC1(transfers.getTransfersFromStop(stopIndex.toStopIndex(A)))).containsExactly(
      200,
      400
    );
  }

  @Test
  void raptorTransfersAreCachedByPreferences() {
    var provider = new FakeTransferPathProvider();
    provider.addNearby(A, B, 100, 60);
    var snapshot = generate(stopIndex(), provider, 1.0);

    // Two equal, but separately created preferences - one is mapped for each request
    var first = snapshot.raptorTransfers(withReluctance(1.0));
    var second = snapshot.raptorTransfers(withReluctance(1.0));

    assertThat(second).isSameInstanceAs(first);
  }

  @Test
  void aNewSnapshotStartsWithAnEmptyCache() {
    var provider = new FakeTransferPathProvider();
    provider.addNearby(A, B, 100, 60);
    var stopIndex = stopIndex();
    var fromA = stopIndex.toStopIndex(A);
    var original = generate(stopIndex, provider, 1.0);
    var walkPreferences = withReluctance(1.0);
    var originalTransfers = original.raptorTransfers(walkPreferences);

    // Commit a new path A -> C
    var lifecycle = new RegularTransferRepositoryLifecycle<>(stopIndex, provider);
    var repository = lifecycle.copyOnWrite(original);
    repository.setPath(
      walkPreferences,
      fromA,
      stopIndex.toStopIndex(C),
      new FakeTransferPathProvider.FakePath(A, C, 200, 90)
    );
    var updated = lifecycle.freeze(repository);
    var updatedTransfers = updated.raptorTransfers(walkPreferences);

    assertThat(updatedTransfers).isNotSameInstanceAs(originalTransfers);
    assertThat(collectC1(updatedTransfers.getTransfersFromStop(fromA))).containsExactly(100, 200);
    assertThat(collectC1(originalTransfers.getTransfersFromStop(fromA))).containsExactly(100);
  }
}
