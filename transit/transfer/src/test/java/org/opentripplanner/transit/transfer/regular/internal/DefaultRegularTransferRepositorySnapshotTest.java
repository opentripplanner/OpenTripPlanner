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
import org.opentripplanner.raptor.data.stop.RaptorStopIndex;
import org.opentripplanner.raptor.data.transfers.regular.RaptorRegularTransferService;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.transit.transfer.regular.FakeRequestMapper;
import org.opentripplanner.transit.transfer.regular.FakeTransferPathProvider;
import org.opentripplanner.transit.transfer.regular.FakeTransferPathProvider.FakePath;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.api.maxduration.MaxDurationsPreference;
import org.opentripplanner.transit.transfer.regular.api.parameters.DeduplicationLimit;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferParameters;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfile;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;
import org.opentripplanner.transit.transfer.regular.api.parameters.WarmupParameters;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.api.report.UserPreferenceSource;
import org.opentripplanner.transit.transfer.regular.spi.PathCriteria;
import org.opentripplanner.transit.transfer.regular.spi.TransferPath;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;
import org.opentripplanner.transit.transfer.regular.spi.TransferRequestMapper;
import org.opentripplanner.transit.transfer.regular.spi.TransferSearch;

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
  private RegularTransferRepositoryLifecycle<FakePath, WalkPreferences> lifecycle;
  private RegularTransferRepositorySnapshot<FakePath, WalkPreferences> initial;

  /** Graph build: A -> B, A -> C, B -> C */
  @BeforeEach
  void setUp() {
    var buildRepository = new DefaultRegularTransferBuildRepository<FakePath>();
    buildRepository.setPaths(PROFILE, A, Map.of(B, path(A, B, 100), C, path(A, C, 200)));
    buildRepository.setPaths(PROFILE, B, Map.of(C, path(B, C, 300)));
    repository =
      (DefaultRegularTransferRepository<FakePath>) buildRepository.createRuntimeRepository();
    lifecycle = new RegularTransferRepositoryLifecycle<>(
      provider,
      FakeRequestMapper.INSTANCE,
      new TransferParameters<>(
        List.of(new TransferProfile<>("walk", TransferProfileType.WALK, PROFILE, PROFILE))
      ),
      stopIndex().size()
    );
    initial = freeze();
  }

  @Test
  void raptorTransfersAreRecostedUnderTheRequestPreferences() {
    var transfers = initial.forRequest(REQUEST).raptorTransfers();

    // Cost was re-costed under the request's own reluctance (2.0), not the build-time one.
    assertThat(from(transfers, A)).containsExactly(tx(B, 200), tx(C, 400));
    assertThat(from(transfers, B)).containsExactly(tx(C, 600));
    assertThat(to(transfers, C)).containsExactly(tx(A, 400), tx(B, 600));
  }

  @Test
  void theSearchIsCreatedOncePerCreateAndPerPatch() {
    initial.forRequest(REQUEST).raptorTransfers();

    assertThat(provider.searches).isEqualTo(1);
    assertThat(provider.recosted).isEqualTo(3);

    repository.replaceTransfersFrom(PROFILE, A, Map.of(C, path(A, C, 250), D, path(A, D, 50)));
    freeze().forRequest(REQUEST).raptorTransfers();

    assertThat(provider.searches).isEqualTo(2);
    assertThat(provider.recosted).isEqualTo(5);
  }

  @Test
  void theRequestIsMappedOnceForAllTheTransfersOfTheRequest() {
    var mapper = new CountingRequestMapper();
    var buildRepository = new DefaultRegularTransferBuildRepository<FakePath>();
    buildRepository.setPaths(PROFILE, A, Map.of(B, path(A, B, 100)));
    var lifecycle = new RegularTransferRepositoryLifecycle<>(
      provider,
      mapper,
      new TransferParameters<>(
        List.of(new TransferProfile<>("walk", TransferProfileType.WALK, PROFILE, PROFILE))
      ),
      stopIndex().size()
    );
    var snapshot = lifecycle.freeze(buildRepository.createRuntimeRepository());

    var transfers = snapshot.forRequest(REQUEST);
    transfers.raptorTransfers();
    transfers.raptorTransfers();
    assertThat(transfers.findPath(A, B)).isEqualTo(path(A, B, 100));
    assertThat(transfers.findPath(A, C)).isNull();

    assertThat(mapper.toUserPreferencesCalls).isEqualTo(1);
  }

  /**
   * Two profiles with their own paths: {@code far} (reluctance 10) has the path A -> C, and
   * {@code near} (reluctance 1) has the path A -> B. The warmup entry, with the reluctance 3 and
   * {@code far} as its parent, is closer to a request with the reluctance 2.5 than {@code near}.
   */
  private RegularTransferRepositorySnapshot<FakePath, WalkPreferences> snapshotWithWarmup(
    boolean withWarmupEntry
  ) {
    return snapshotWithWarmup(withWarmupEntry, false);
  }

  private RegularTransferRepositorySnapshot<FakePath, WalkPreferences> snapshotWithWarmup(
    boolean withWarmupEntry,
    boolean collectCacheStatistics
  ) {
    var near = withReluctance(1.0);
    var far = withReluctance(10.0);
    var buildRepository = new DefaultRegularTransferBuildRepository<FakePath>();
    buildRepository.setPaths(near, A, Map.of(B, path(A, B, 100)));
    buildRepository.setPaths(far, A, Map.of(C, path(A, C, 200)));
    var warmup = withWarmupEntry
      ? List.of(new WarmupParameters("warm", "far", withReluctance(3.0)))
      : List.<WarmupParameters>of();
    var lifecycle = new RegularTransferRepositoryLifecycle<>(
      provider,
      FakeRequestMapper.INSTANCE,
      new TransferParameters<>(
        List.of(
          new TransferProfile<>("near", TransferProfileType.WALK, near, near),
          new TransferProfile<>("far", TransferProfileType.WALK, far, far)
        ),
        Map.of(TransferProfileType.WALK, MaxDurationsPreference.DEFAULT),
        DeduplicationLimit.DEFAULT,
        warmup
      ),
      stopIndex().size(),
      collectCacheStatistics
    );
    return lifecycle.freeze(buildRepository.createRuntimeRepository());
  }

  @Test
  void warmUpCreatesTheTransfersOfTheProfilesAndTheWarmupEntriesBeforeTheFirstRequest() {
    var snapshot = snapshotWithWarmup(true);
    assertThat(provider.searches).isEqualTo(0);

    snapshot.warmUp();

    // The profiles 'near' and 'far', and the warmup entry
    assertThat(provider.searches).isEqualTo(3);
    // The first request with the preferences of the entry does not create the transfers again
    var transfers = snapshot.forRequest(withReluctance(3.0)).raptorTransfers();
    assertThat(from(transfers, A)).containsExactly(tx(C, 600));
    assertThat(provider.searches).isEqualTo(3);
  }

  @Test
  void warmUpCreatesTheTransfersOfTheProfilesEvenWithoutWarmupEntries() {
    var snapshot = snapshotWithWarmup(false);

    snapshot.warmUp();

    assertThat(provider.searches).isEqualTo(2);
    // A request with the preferences of a profile does not create the transfers again
    var near = snapshot.forRequest(withReluctance(1.0)).raptorTransfers();
    var far = snapshot.forRequest(withReluctance(10.0)).raptorTransfers();
    assertThat(provider.searches).isEqualTo(2);
    assertThat(from(near, A)).containsExactly(tx(B, 100));
    assertThat(from(far, A)).containsExactly(tx(C, 2000));
  }

  @Test
  void withoutWarmUpTheTransfersAreCreatedByTheFirstRequest() {
    var snapshot = snapshotWithWarmup(true);

    snapshot.forRequest(withReluctance(1.0)).raptorTransfers();

    assertThat(provider.searches).isEqualTo(1);
  }

  @Test
  void aRequestCloserToAWarmupEntryThanToAProfileUsesTheParentsPaths() {
    var snapshot = snapshotWithWarmup(true);

    var transfers = snapshot.forRequest(withReluctance(2.5));

    // The warmup entry (reluctance 3) is closer than the profile 'near' (reluctance 1)
    assertThat(transfers.findPath(A, C)).isEqualTo(path(A, C, 200));
    assertThat(transfers.findPath(A, B)).isNull();
  }

  @Test
  void withoutTheWarmupEntryTheClosestProfileIsUsed() {
    var snapshot = snapshotWithWarmup(false);

    var transfers = snapshot.forRequest(withReluctance(2.5));

    assertThat(transfers.findPath(A, B)).isEqualTo(path(A, B, 100));
    assertThat(transfers.findPath(A, C)).isNull();
  }

  @Test
  void aProfileThatMatchesExactlyIsUsedEvenIfAWarmupEntryIsClose() {
    var snapshot = snapshotWithWarmup(true);

    var transfers = snapshot.forRequest(withReluctance(1.0));

    assertThat(transfers.findPath(A, B)).isEqualTo(path(A, B, 100));
  }

  @Test
  void raptorTransfersAreCachedByPreferences() {
    // Two equal, but separately created preferences - one is mapped for each request
    var first = initial.forRequest(withReluctance(2.0)).raptorTransfers();
    var second = initial.forRequest(withReluctance(2.0)).raptorTransfers();

    assertThat(second).isSameInstanceAs(first);
  }

  @Test
  void aSnapshotWithoutChangesKeepsTheCachedTransfers() {
    var transfers = initial.forRequest(REQUEST).raptorTransfers();

    var next = freeze();

    assertThat(next.forRequest(REQUEST).raptorTransfers()).isSameInstanceAs(transfers);
  }

  @Test
  void aSnapshotPatchesTheTransfersOfTheChangedStopsOnly() {
    var initialTransfers = initial.forRequest(REQUEST).raptorTransfers();
    provider.recosted = 0;

    repository.replaceTransfersFrom(PROFILE, A, Map.of(C, path(A, C, 250), D, path(A, D, 50)));
    var next = freeze();
    var transfers = next.forRequest(REQUEST).raptorTransfers();

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
    initial.forRequest(REQUEST).raptorTransfers();

    repository.replaceTransfersFrom(PROFILE, A, Map.of());
    freeze();
    repository.replaceTransfersFrom(PROFILE, C, Map.of(A, path(C, A, 10)));
    freeze();
    repository.resetTransfersFrom(PROFILE, A);
    var last = freeze();
    provider.recosted = 0;
    var transfers = last.forRequest(REQUEST).raptorTransfers();

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

    var transfers = next.forRequest(REQUEST).raptorTransfers();

    assertThat(from(transfers, A)).isEmpty();
    assertThat(from(transfers, B)).containsExactly(tx(C, 600));
  }

  @Test
  void aRolledBackChangeDoesNotChangeTheCachedTransfers() {
    var transfers = initial.forRequest(REQUEST).raptorTransfers();

    repository.replaceTransfersFrom(PROFILE, A, Map.of());
    // Rollback: the repository is not frozen, the next write continues from the last snapshot
    repository = continueFrom(initial);
    var next = freeze();

    assertThat(next.forRequest(REQUEST).raptorTransfers()).isSameInstanceAs(transfers);
    assertThat(from(next.forRequest(REQUEST).raptorTransfers(), A)).containsExactly(
      tx(B, 200),
      tx(C, 400)
    );
  }

  @Test
  void aChangeWhoseCommitFailedAfterItWasFrozenIsRolledBack() {
    initial.forRequest(REQUEST).raptorTransfers();

    repository.replaceTransfersFrom(PROFILE, A, Map.of());
    // The commit fails after this freeze, so the frozen snapshot is never published
    lifecycle.freeze(repository);

    // The framework passes the last published snapshot to the next write
    repository = continueFrom(initial);
    repository.replaceTransfersFrom(PROFILE, B, Map.of(D, path(B, D, 10)));
    var next = freeze();
    provider.recosted = 0;
    var transfers = next.forRequest(REQUEST).raptorTransfers();

    assertThat(next.forRequest(REQUEST).findPath(A, B)).isEqualTo(path(A, B, 100));
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

    assertThat(next.forRequest(REQUEST).findPath(A, D)).isSameInstanceAs(newPath);
    assertThat(next.forRequest(REQUEST).findPath(A, B)).isNull();
    assertThat(initial.forRequest(REQUEST).findPath(A, B)).isEqualTo(path(A, B, 100));
  }

  /**
   * Publish the paths of the repository, and continue with the repository of the next
   * transaction, as the framework does.
   */
  private RegularTransferRepositorySnapshot<FakePath, WalkPreferences> freeze() {
    var snapshot = lifecycle.freeze(repository);
    repository = continueFrom(snapshot);
    return snapshot;
  }

  private DefaultRegularTransferRepository<FakePath> continueFrom(
    RegularTransferRepositorySnapshot<FakePath, WalkPreferences> snapshot
  ) {
    return (DefaultRegularTransferRepository<FakePath>) lifecycle.copyOnWrite(snapshot);
  }

  /** Compare patched transfers with transfers created from scratch from the same paths. */
  private void assertSameAsCreatedFromScratch(
    RegularTransferRepositorySnapshot<FakePath, WalkPreferences> snapshot,
    RaptorRegularTransferService transfers
  ) {
    var paths = (
      (DefaultRegularTransferRepositorySnapshot<FakePath, WalkPreferences>) snapshot
    ).paths();
    var expected = new RaptorTransfersFactory<>(
      N_STOPS,
      provider,
      FakeRequestMapper.INSTANCE,
      Map.<AbstractUserPreferences<?>, WalkPreferences>of(PROFILE, PROFILE)
    ).create(paths, REQUEST, PROFILE);
    for (int stop = 0; stop < N_STOPS; ++stop) {
      assertThat(from(transfers, stop)).containsExactlyElementsIn(from(expected, stop));
      assertThat(to(transfers, stop)).containsExactlyElementsIn(to(expected, stop));
    }
  }

  private static RaptorStopIndex stopIndex() {
    return TestStopIndex.of(
      IntStream.range(0, N_STOPS)
        .mapToObj(i -> FeedScopedId.of("F", "S" + i))
        .toList()
    );
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

  private record Tx(int stop, int c1) {}

  /** Counts the mapping of requests to user preferences. */
  private static final class CountingRequestMapper
    implements TransferRequestMapper<WalkPreferences>
  {

    private int toUserPreferencesCalls = 0;

    @Override
    public WalkPreferences toRequest(
      AbstractUserPreferences<?> userPreferences,
      WalkPreferences base
    ) {
      return FakeRequestMapper.INSTANCE.toRequest(userPreferences, base);
    }

    @Override
    public AbstractUserPreferences<?> toUserPreferences(WalkPreferences request) {
      ++toUserPreferencesCalls;
      return FakeRequestMapper.INSTANCE.toUserPreferences(request);
    }

    @Override
    public AbstractUserPreferences<?> toUserPreferences(
      TransferProfileType type,
      WalkPreferences request
    ) {
      return FakeRequestMapper.INSTANCE.toUserPreferences(type, request);
    }
  }

  /** Counts the created searches and the re-costed paths. */
  private static final class CountingPathProvider
    implements TransferPathProvider<FakePath, WalkPreferences>
  {

    private final FakeTransferPathProvider delegate = new FakeTransferPathProvider();
    private int searches = 0;
    private int recosted = 0;

    @Override
    public TransferSearch<FakePath> searchFor(WalkPreferences request) {
      ++searches;
      var search = delegate.searchFor(request);
      return new TransferSearch<>() {
        @Override
        public Collection<TransferPath<FakePath>> findNearbyStops(
          FeedScopedId fromStop,
          MaxDurationsPreference maxDurations
        ) {
          return search.findNearbyStops(fromStop, maxDurations);
        }

        @Override
        public Optional<PathCriteria> computePathCriteria(FakePath path) {
          ++recosted;
          return search.computePathCriteria(path);
        }
      };
    }
  }

  @Test
  void theCacheReportHasTheKindAndTheNumberOfRequestsOfEachKey() {
    var snapshot = snapshotWithWarmup(true, true);
    snapshot.warmUp();

    snapshot.forRequest(withReluctance(1.0)).raptorTransfers();
    snapshot.forRequest(withReluctance(1.0)).raptorTransfers();
    snapshot.forRequest(withReluctance(3.0)).raptorTransfers();
    snapshot.forRequest(withReluctance(2.5)).raptorTransfers();

    var report = snapshot.cacheReport().entries();

    assertThat(report).hasSize(3);
    var near = report
      .stream()
      .filter(e -> "near".equals(e.id()))
      .findFirst()
      .orElseThrow();
    assertThat(near.source()).isEqualTo(UserPreferenceSource.PROFILE);
    assertThat(near.hits()).isEqualTo(2);
    assertThat(near.cached()).isTrue();
    var warm = report
      .stream()
      .filter(e -> "warm".equals(e.id()))
      .findFirst()
      .orElseThrow();
    assertThat(warm.source()).isEqualTo(UserPreferenceSource.WARMUP);
    assertThat(warm.closestProfileId()).isEqualTo("far");
    assertThat(warm.hits()).isEqualTo(1);
    var onDemand = report
      .stream()
      .filter(e -> e.source() == UserPreferenceSource.ON_DEMAND)
      .findFirst()
      .orElseThrow();
    assertThat(onDemand.id()).isNull();
    assertThat(onDemand.closestProfileId()).isEqualTo("far");
    assertThat(onDemand.hits()).isEqualTo(1);
    // The most used key is first
    assertThat(report.getFirst().id()).isEqualTo("near");
  }

  @Test
  void theCacheReportIsEmptyIfTheStatisticsAreNotCollected() {
    var snapshot = snapshotWithWarmup(true, false);

    snapshot.forRequest(withReluctance(1.0)).raptorTransfers();

    assertThat(snapshot.cacheReport().entries()).isEmpty();
  }
}
