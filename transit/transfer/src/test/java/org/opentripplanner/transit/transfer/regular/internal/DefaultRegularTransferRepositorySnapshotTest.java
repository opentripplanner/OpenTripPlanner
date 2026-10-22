package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.raptor.data.stop.StopIndex;
import org.opentripplanner.raptor.data.transfers.regular.RaptorRegularTransferService;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.transit.transfer.regular.FakeTransferPathProvider;
import org.opentripplanner.transit.transfer.regular.FakeTransferPathProvider.FakePath;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.api.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.spi.PathCriteria;
import org.opentripplanner.transit.transfer.regular.spi.TransferPath;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;

/**
 * The Raptor transfers of a snapshot: derived from its paths under the request-time preferences,
 * cached, and carried forward to the next snapshot - patched for the stops whose transfers
 * changed.
 */
class DefaultRegularTransferRepositorySnapshotTest {

  /** Enough stops for a change of a few stops to be patched, see {@link RaptorTransfersFactory}. */
  private static final int N_STOPS = 100;

  private static final int A = 0;
  private static final int B = 1;
  private static final int C = 2;
  private static final int D = 3;

  private static final WalkPreferences PROFILE = withReluctance(1.0);
  private static final WalkPreferences REQUEST = withReluctance(2.0);

  private final CountingPathProvider provider = new CountingPathProvider();
  private DefaultRegularTransferRepository<FakePath> repository;
  private RegularTransferRepositoryLifecycle<FakePath> lifecycle;
  private RegularTransferRepositorySnapshot<FakePath> initial;

  /** Graph build: A -> B, A -> C, B -> C */
  @BeforeEach
  void setUp() {
    var buildRepository = new DefaultRegularTransferBuildRepository<FakePath>();
    buildRepository.setPaths(PROFILE, A, Map.of(B, path(A, B, 100), C, path(A, C, 200)));
    buildRepository.setPaths(PROFILE, B, Map.of(C, path(B, C, 300)));
    repository =
      (DefaultRegularTransferRepository<FakePath>) buildRepository.createRuntimeRepository();
    lifecycle = new RegularTransferRepositoryLifecycle<>(stopIndex(), provider);
    initial = freeze();
  }

  @Test
  void raptorTransfersAreRecostedUnderTheRequestPreferences() {
    var transfers = initial.raptorTransfers(REQUEST);

    // Cost was re-costed under the request's own reluctance (2.0), not the build-time one.
    assertThat(from(transfers, A)).containsExactly(tx(B, 200), tx(C, 400));
    assertThat(from(transfers, B)).containsExactly(tx(C, 600));
    assertThat(to(transfers, C)).containsExactly(tx(A, 400), tx(B, 600));
  }

  @Test
  void raptorTransfersAreCachedByPreferences() {
    // Two equal, but separately created preferences - one is mapped for each request
    var first = initial.raptorTransfers(withReluctance(2.0));
    var second = initial.raptorTransfers(withReluctance(2.0));

    assertThat(second).isSameInstanceAs(first);
  }

  @Test
  void aSnapshotWithoutChangesKeepsTheCachedTransfers() {
    var transfers = initial.raptorTransfers(REQUEST);

    var next = freeze();

    assertThat(next.raptorTransfers(REQUEST)).isSameInstanceAs(transfers);
  }

  @Test
  void aSnapshotPatchesTheTransfersOfTheChangedStopsOnly() {
    var initialTransfers = initial.raptorTransfers(REQUEST);
    provider.recosted = 0;

    repository.replaceTransfersFrom(PROFILE, A, Map.of(C, path(A, C, 250), D, path(A, D, 50)));
    var next = freeze();
    var transfers = next.raptorTransfers(REQUEST);

    // Only the two new paths from A are re-costed
    assertThat(provider.recosted).isEqualTo(2);
    assertThat(from(transfers, A)).containsExactly(tx(C, 500), tx(D, 100));
    assertThat(from(transfers, B)).containsExactly(tx(C, 600));
    assertThat(to(transfers, B)).isEmpty();
    assertThat(to(transfers, C)).containsExactly(tx(B, 600), tx(A, 500));
    assertThat(to(transfers, D)).containsExactly(tx(A, 100));
    assertSameAsCreatedFromScratch(next, transfers);
    // The transfers of the previous snapshot are not changed
    assertThat(from(initialTransfers, A)).containsExactly(tx(B, 200), tx(C, 400));
  }

  @Test
  void transfersNotUsedForSeveralSnapshotsArePatchedForAllChanges() {
    initial.raptorTransfers(REQUEST);

    repository.replaceTransfersFrom(PROFILE, A, Map.of());
    freeze();
    repository.replaceTransfersFrom(PROFILE, C, Map.of(A, path(C, A, 10)));
    freeze();
    repository.resetTransfersFrom(PROFILE, A);
    var last = freeze();
    provider.recosted = 0;
    var transfers = last.raptorTransfers(REQUEST);

    // A is back to its two graph-build paths, plus the new path from C
    assertThat(provider.recosted).isEqualTo(3);
    assertThat(from(transfers, A)).containsExactly(tx(B, 200), tx(C, 400));
    assertThat(from(transfers, C)).containsExactly(tx(A, 20));
    assertSameAsCreatedFromScratch(last, transfers);
  }

  @Test
  void transfersFirstUsedAfterAChangeAreCreatedFromScratch() {
    repository.replaceTransfersFrom(PROFILE, A, Map.of());
    var next = freeze();

    var transfers = next.raptorTransfers(REQUEST);

    assertThat(from(transfers, A)).isEmpty();
    assertThat(from(transfers, B)).containsExactly(tx(C, 600));
  }

  @Test
  void aRolledBackChangeDoesNotChangeTheCachedTransfers() {
    var transfers = initial.raptorTransfers(REQUEST);

    repository.replaceTransfersFrom(PROFILE, A, Map.of());
    // Rollback: the repository is not frozen, the next write continues from the last snapshot
    repository = continueFrom(initial);
    var next = freeze();

    assertThat(next.raptorTransfers(REQUEST)).isSameInstanceAs(transfers);
    assertThat(from(next.raptorTransfers(REQUEST), A)).containsExactly(tx(B, 200), tx(C, 400));
  }

  @Test
  void aChangeWhoseCommitFailedAfterItWasFrozenIsRolledBack() {
    initial.raptorTransfers(REQUEST);

    repository.replaceTransfersFrom(PROFILE, A, Map.of());
    // The commit fails after this freeze, so the frozen snapshot is never published
    lifecycle.freeze(repository);

    // The framework passes the last published snapshot to the next write
    repository = continueFrom(initial);
    repository.replaceTransfersFrom(PROFILE, B, Map.of(D, path(B, D, 10)));
    var next = freeze();
    provider.recosted = 0;
    var transfers = next.raptorTransfers(REQUEST);

    assertThat(next.findPath(REQUEST, A, B)).isEqualTo(path(A, B, 100));
    assertThat(from(transfers, A)).containsExactly(tx(B, 200), tx(C, 400));
    assertThat(from(transfers, B)).containsExactly(tx(D, 20));
    // Patched from the transfers of the published snapshot, for the change of B only
    assertThat(provider.recosted).isEqualTo(1);
    assertSameAsCreatedFromScratch(next, transfers);
  }

  @Test
  void findPathReadsTheReplacedPaths() {
    var newPath = path(A, D, 50);

    repository.replaceTransfersFrom(PROFILE, A, Map.of(D, newPath));
    var next = freeze();

    assertThat(next.findPath(REQUEST, A, D)).isSameInstanceAs(newPath);
    assertThat(next.findPath(REQUEST, A, B)).isNull();
    assertThat(initial.findPath(REQUEST, A, B)).isEqualTo(path(A, B, 100));
  }

  /**
   * Publish the paths of the repository, and continue with the repository of the next
   * transaction, as the framework does.
   */
  private RegularTransferRepositorySnapshot<FakePath> freeze() {
    var snapshot = lifecycle.freeze(repository);
    repository = continueFrom(snapshot);
    return snapshot;
  }

  private DefaultRegularTransferRepository<FakePath> continueFrom(
    RegularTransferRepositorySnapshot<FakePath> snapshot
  ) {
    return (DefaultRegularTransferRepository<FakePath>) lifecycle.copyOnWrite(snapshot);
  }

  /** Compare patched transfers with transfers created from scratch from the same paths. */
  private void assertSameAsCreatedFromScratch(
    RegularTransferRepositorySnapshot<FakePath> snapshot,
    RaptorRegularTransferService transfers
  ) {
    var paths = ((DefaultRegularTransferRepositorySnapshot<FakePath>) snapshot).paths();
    var expected = new RaptorTransfersFactory<>(N_STOPS, provider).create(paths, REQUEST, PROFILE);
    for (int stop = 0; stop < N_STOPS; ++stop) {
      assertThat(from(transfers, stop)).containsExactlyElementsIn(from(expected, stop));
      assertThat(to(transfers, stop)).containsExactlyElementsIn(to(expected, stop));
    }
  }

  private static StopIndex stopIndex() {
    var stops = IntStream.range(0, N_STOPS)
      .mapToObj(i -> new TestStop(FeedScopedId.of("F", "S" + i), i))
      .toList();
    return new StopIndex(N_STOPS, stops, TestStop::index, TestStop::id);
  }

  private static FakePath path(int from, int to, int cost) {
    return new FakePath(FeedScopedId.of("F", "S" + from), FeedScopedId.of("F", "S" + to), cost, 60);
  }

  private static WalkPreferences withReluctance(double reluctance) {
    return WalkPreferences.of().withReluctance(Reluctance.of(reluctance)).build();
  }

  private static Tx tx(int stop, int c1) {
    return new Tx(stop, c1);
  }

  private static List<Tx> from(RaptorRegularTransferService transfers, int stop) {
    return collect(transfers.getTransfersFromStop(stop));
  }

  private static List<Tx> to(RaptorRegularTransferService transfers, int stop) {
    return collect(transfers.getTransfersToStop(stop));
  }

  private static List<Tx> collect(Iterator<? extends RaptorTransfer> it) {
    List<Tx> result = new ArrayList<>();
    while (it.hasNext()) {
      var transfer = it.next();
      result.add(new Tx(transfer.stop(), transfer.c1()));
    }
    return result;
  }

  private record TestStop(FeedScopedId id, int index) {}

  private record Tx(int stop, int c1) {}

  /** Counts the re-costed paths. */
  private static final class CountingPathProvider
    implements TransferPathProvider<FakePath, AbstractUserPreferences<?>>
  {

    private final FakeTransferPathProvider delegate = new FakeTransferPathProvider();
    private int recosted = 0;

    @Override
    public Collection<TransferPath<FakePath>> findNearbyStops(
      FeedScopedId fromStop,
      AbstractUserPreferences<?> userPreferences
    ) {
      return delegate.findNearbyStops(fromStop, userPreferences);
    }

    @Override
    public Optional<PathCriteria> computePathCriteria(
      FakePath path,
      AbstractUserPreferences<?> userPreferences
    ) {
      ++recosted;
      return delegate.computePathCriteria(path, userPreferences);
    }
  }
}
