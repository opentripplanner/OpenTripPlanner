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
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfileType;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfiles;

class DefaultRegularTransferServiceFactoryTest {

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
    var repository = new DefaultRegularTransferRepository<FakeTransferPathProvider.FakePath>();
    AbstractUserPreferences<?> preferences = withReluctance(buildTimeReluctance);
    // configuredRequestParameters (R) is unread by DefaultTransferGenerator today - reuse the
    // preferences instance as a type-compatible placeholder. Explicit type witnesses throughout,
    // since diamond inference from a wildcard-typed argument triggers capture conversion and
    // produces a fresh, non-matching captured type at each use.
    TransferProfile<AbstractUserPreferences<?>> profile = new TransferProfile<
      AbstractUserPreferences<?>
    >(TransferProfileType.WALK, preferences, preferences);
    TransferProfiles<AbstractUserPreferences<?>> profiles = TransferProfiles.<
      AbstractUserPreferences<?>
    >of(List.of(profile));
    new DefaultTransferGenerator<FakeTransferPathProvider.FakePath, AbstractUserPreferences<?>>(
      stopIndex,
      List.of(A),
      provider,
      profiles,
      repository
    ).generateTransfersForAllStops();
    return repository.freeze();
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
    provider.addNearby(TransferProfileType.WALK, A, B, 100, 60);
    provider.addNearby(TransferProfileType.WALK, A, C, 200, 90);

    var stopIndex = stopIndex();
    var repository = generate(stopIndex, provider, 1.0);

    var factory = new DefaultRegularTransferServiceFactory<>(stopIndex, repository, provider);
    var service = factory.create(TransferProfileType.WALK, withReluctance(2.0));

    // Cost was re-costed under the request's own reluctance (2.0), not the build-time one.
    assertThat(collectC1(service.getTransfersFromStop(stopIndex.toStopIndex(A)))).containsExactly(
      200,
      400
    );
  }

  @Test
  void cachesByModeAndPreferences() {
    var provider = new FakeTransferPathProvider();
    provider.addNearby(TransferProfileType.WALK, A, B, 100, 60);
    var stopIndex = stopIndex();
    var repository = generate(stopIndex, provider, 1.0);
    var factory = new DefaultRegularTransferServiceFactory<>(stopIndex, repository, provider);

    var prefs = withReluctance(1.0);
    var first = factory.create(TransferProfileType.WALK, prefs);
    var second = factory.create(TransferProfileType.WALK, prefs);

    assertThat(first).isSameInstanceAs(second);
  }
}
