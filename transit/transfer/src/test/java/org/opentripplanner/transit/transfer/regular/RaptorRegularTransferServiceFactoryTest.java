package org.opentripplanner.transit.transfer.regular;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.raptor.data.stop.StopIndex;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.transit.transfer.regular.internal.DefaultTransferGenerator;
import org.opentripplanner.transit.transfer.regular.internal.RegularTransferRepository;
import org.opentripplanner.transit.transfer.regular.spi.RegularTransferParameters;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.transit.transfer.regular.FakeTransferPathProvider.FakePrefs;

class RaptorRegularTransferServiceFactoryTest {

  private static final FeedScopedId A = FeedScopedId.of("F", "A");
  private static final FeedScopedId B = FeedScopedId.of("F", "B");
  private static final FeedScopedId C = FeedScopedId.of("F", "C");

  private record TestStop(FeedScopedId id, int index) {}

  private static StopIndex stopIndex() {
    var stops = List.of(new TestStop(A, 0), new TestStop(B, 1), new TestStop(C, 2));
    return new StopIndex(stops.size(), stops, TestStop::index, TestStop::id);
  }

  /** Populates the repository the same way graph build would, via the real generator. */
  private static RegularTransferRepository<FakeTransferPathProvider.FakePath> generate(
    StopIndex stopIndex,
    FakeTransferPathProvider provider,
    FakeTransferPathProvider.FakePrefs buildTimePrefs
  ) {
    var repository = new RegularTransferRepository<FakeTransferPathProvider.FakePath>();
    var profile = new RegularTransferParameters<>(RaptorTransferProfile.WALK, null, buildTimePrefs);
    new DefaultTransferGenerator<>(
      stopIndex,
      List.of(A),
      provider,
      List.of(profile),
      baseCost -> 0,
      repository
    ).generateTransfersForAllStops();
    return repository;
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
  void buildsAllStoredCandidatesRecostedUnderRequestPreferences() {
    var provider = new FakeTransferPathProvider();
    provider.addNearby(RaptorTransferProfile.WALK, A, B, 100, 60);
    provider.addNearby(RaptorTransferProfile.WALK, A, C, 200, 90);

    var stopIndex = stopIndex();
    var repository = generate(stopIndex, provider, new FakeTransferPathProvider.FakePrefs(1.0, 120));

    var factory = new RaptorRegularTransferServiceFactory<>(stopIndex, repository, provider);
    var service = factory.create(RaptorTransferProfile.WALK, new FakePrefs(2.0, 120));

    // Cost was re-costed under the request's own costMultiplier (2.0), not the build-time one.
    assertThat(collectC1(service.getTransfersFromStop(stopIndex.toStopIndex(A)))).containsExactly(200, 400);
  }

  @Test
  void cachesByModeAndPreferences() {
    var provider = new FakeTransferPathProvider();
    provider.addNearby(RaptorTransferProfile.WALK, A, B, 100, 60);
    var stopIndex = stopIndex();
    var repository = generate(stopIndex, provider, new FakePrefs(1.0, 120));
    var factory = new RaptorRegularTransferServiceFactory<>(stopIndex, repository, provider);

    var prefs = new FakePrefs(1.0, 120);
    var first = factory.create(RaptorTransferProfile.WALK, prefs);
    var second = factory.create(RaptorTransferProfile.WALK, prefs);

    assertThat(first).isSameInstanceAs(second);
  }
}
