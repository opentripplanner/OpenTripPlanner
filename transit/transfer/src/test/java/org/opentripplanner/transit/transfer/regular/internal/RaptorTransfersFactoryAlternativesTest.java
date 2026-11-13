package org.opentripplanner.transit.transfer.regular.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.transfer.regular.AlternativePath;
import org.opentripplanner.transit.transfer.regular.FakeRequestMapper;
import org.opentripplanner.transit.transfer.regular.FakeTransferPathProvider;
import org.opentripplanner.transit.transfer.regular.FakeTransferPathProvider.FakePath;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WalkPreferences;

/** The choice of the path of a transfer when closable facility ids are closed. */
class RaptorTransfersFactoryAlternativesTest {

  private static final FeedScopedId A = FeedScopedId.of("F", "A");
  private static final FeedScopedId B = FeedScopedId.of("F", "B");
  private static final WalkPreferences PROFILE = WalkPreferences.of()
    .withReluctance(Reluctance.of(1.0))
    .build();
  private static final int FROM = 0;
  private static final int TO = 1;

  private final FakeTransferPathProvider provider = new FakeTransferPathProvider();
  private final FakePath best = new FakePath(A, B, 100, 60, Set.of("e1"));
  private final FakePath detour = new FakePath(A, B, 300, 90);
  private final FakePath usesE2 = new FakePath(A, B, 200, 70, Set.of("e2"));

  @Test
  void theBestPathIsUsedIfNothingIsClosed() {
    assertEquals(List.of(100), costs(Set.of()));
  }

  @Test
  void theCheapestAlternativeThatDoesNotDependOnAClosedIdIsUsed() {
    assertEquals(List.of(200), costs(Set.of("e1")));
    assertEquals(List.of(300), costs(Set.of("e1", "e2")));
  }

  @Test
  void theBestPathIsUsedIfAnotherIdIsClosed() {
    assertEquals(List.of(100), costs(Set.of("e2")));
  }

  @Test
  void thereIsNoTransferIfEveryPathDependsOnAClosedId() {
    var paths = paths(List.of(alternative(best, "e1"), alternative(usesE2, "e2")));
    assertEquals(List.of(), costs(paths, Set.of("e1", "e2")));
  }

  @Test
  void aTransferWithoutAlternativePathsIsUsedWhateverIsClosed() {
    var paths = new TemplatePathsByUserProfile<FakePath>();
    paths.setPaths(PROFILE, FROM, Map.of(TO, detour));

    assertEquals(List.of(300), costs(new OverlaidTemplatePaths<>(paths), Set.of("e1")));
  }

  @Test
  void theClosedIdsAreEmptyUntilTheyAreProvidedAtRuntime() {
    assertTrue(RaptorTransfersFactory.CLOSED_FACILITY_IDS.isEmpty());
  }

  private List<Integer> costs(Set<String> closedIds) {
    var paths = paths(
      List.of(alternative(best, "e1"), alternative(usesE2, "e2"), alternative(detour))
    );
    return costs(paths, closedIds);
  }

  private List<Integer> costs(OverlaidTemplatePaths<FakePath> paths, Set<String> closedIds) {
    var factory = new RaptorTransfersFactory<>(
      2,
      provider,
      FakeRequestMapper.INSTANCE,
      Map.<AbstractUserPreferences<?>, WalkPreferences>of(PROFILE, PROFILE),
      closedIds
    );
    var transfers = factory.create(paths, PROFILE, PROFILE);
    var result = new ArrayList<Integer>();
    transfers.getTransfersFromStop(FROM).forEachRemaining(t -> result.add(t.c1()));
    return result;
  }

  /** The first path is the best path of the transfer. */
  private static OverlaidTemplatePaths<FakePath> paths(List<AlternativePath<FakePath>> all) {
    var paths = new TemplatePathsByUserProfile<FakePath>();
    paths.setPaths(PROFILE, FROM, Map.of(TO, all.get(0).path()));
    paths.setAlternativePaths(PROFILE, FROM, Map.of(TO, all));
    return new OverlaidTemplatePaths<>(paths);
  }

  private static AlternativePath<FakePath> alternative(FakePath path, String... ids) {
    return new AlternativePath<>(path, Set.of(ids));
  }
}
