package org.opentripplanner.transit.transfer.regular.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

/**
 * The Raptor transfers of the transfers with alternative paths when closable facility ids are
 * closed: the profile chooses the path, the request only re-costs it.
 */
class RaptorTransfersFactoryAlternativesTest {

  private static final FeedScopedId A = FeedScopedId.of("F", "A");
  private static final FeedScopedId B = FeedScopedId.of("F", "B");
  private static final WalkPreferences PROFILE = WalkPreferences.of()
    .withReluctance(Reluctance.of(1.0))
    .build();
  private static final WalkPreferences REQUEST = WalkPreferences.of()
    .withReluctance(Reluctance.of(2.0))
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
  void theFirstAlternativeThatDoesNotDependOnAClosedIdIsUsed() {
    assertEquals(List.of(200), costs(Set.of("e1")));
    assertEquals(List.of(300), costs(Set.of("e1", "e2")));
  }

  @Test
  void theRequestPreferencesReCostThePathOfTheProfileButDoNotChooseAnother() {
    // Under the request, the detour (600) is cheaper than the path the profile chooses
    provider.withCost(usesE2, 2.0, 1000);

    assertEquals(List.of(1000), costs(defaultPaths(), Set.of("e1"), REQUEST));
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

  private List<Integer> costs(Set<String> closedIds) {
    return costs(defaultPaths(), closedIds);
  }

  /** The alternatives in the cost order of the profile. */
  private OverlaidTemplatePaths<FakePath> defaultPaths() {
    return paths(List.of(alternative(best, "e1"), alternative(usesE2, "e2"), alternative(detour)));
  }

  private List<Integer> costs(OverlaidTemplatePaths<FakePath> paths, Set<String> closedIds) {
    return costs(paths, closedIds, PROFILE);
  }

  private List<Integer> costs(
    OverlaidTemplatePaths<FakePath> paths,
    Set<String> closedIds,
    WalkPreferences request
  ) {
    var factory = new RaptorTransfersFactory<>(
      2,
      provider,
      FakeRequestMapper.INSTANCE,
      Map.<AbstractUserPreferences<?>, WalkPreferences>of(PROFILE, PROFILE)
    );
    var transfers = factory.create(new OpenTemplatePaths<>(paths, closedIds), request, PROFILE);
    var result = new ArrayList<Integer>();
    transfers.getTransfersFromStop(FROM).forEachRemaining(t -> result.add(t.c1()));
    return result;
  }

  /** The paths of the transfer, in the cost order of the profile, the best path first. */
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
