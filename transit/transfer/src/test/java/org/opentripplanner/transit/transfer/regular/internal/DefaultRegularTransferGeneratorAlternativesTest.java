package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import org.opentripplanner.transit.transfer.regular.api.maxduration.MaxDurationsPreference;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferParameters;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfile;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;
import org.opentripplanner.transit.transfer.regular.api.preferences.WalkPreferences;

/** The alternative paths generated for the paths that depend on closable facility ids. */
class DefaultRegularTransferGeneratorAlternativesTest {

  private static final FeedScopedId A = FeedScopedId.of("F", "A");
  private static final FeedScopedId B = FeedScopedId.of("F", "B");
  private static final FeedScopedId C = FeedScopedId.of("F", "C");
  private static final int A_INDEX = 0;
  private static final int B_INDEX = 1;
  private static final int C_INDEX = 2;

  private static final WalkPreferences FIRST = withReluctance(1.0);
  private static final WalkPreferences SECOND = withReluctance(2.0);

  private final FakeTransferPathProvider provider = new FakeTransferPathProvider();

  @Test
  void aPathWithOneClosableFacilityGetsTheDetourAsAlternative() {
    var best = provider.addNearby(A, B, 100, 60, Set.of("e1"));
    var detour = provider.addNearby(A, B, 300, 90);

    var subject = generate(true);

    assertThat(subject.paths(FIRST, B_INDEX)).containsExactly(
      alternative(best, "e1"),
      alternative(detour)
    );
    assertThat(subject.avoidedIdsOfExtraSearches()).containsExactly(Set.of("e1"));
  }

  @Test
  void aPathWithTwoClosableFacilityIdsGetsAnAlternativeForEachNonEmptySubset() {
    var best = provider.addNearby(A, B, 100, 60, Set.of("e1", "e2"));
    var avoidsE1 = provider.addNearby(A, B, 200, 60, Set.of("e2"));
    var avoidsE2 = provider.addNearby(A, B, 250, 60, Set.of("e1"));
    var avoidsBoth = provider.addNearby(A, B, 400, 60);

    var subject = generate(true);

    assertThat(subject.paths(FIRST, B_INDEX)).containsExactly(
      alternative(best, "e1", "e2"),
      alternative(avoidsE1, "e2"),
      alternative(avoidsE2, "e1"),
      alternative(avoidsBoth)
    );
    assertThat(subject.avoidedIdsOfExtraSearches()).containsExactly(
      Set.of("e1"),
      Set.of("e2"),
      Set.of("e1", "e2")
    );
  }

  @Test
  void aPathWithThreeClosableFacilityIdsNeedsOneSearchForEachOfTheSevenSubsets() {
    provider.addNearby(A, B, 100, 60, Set.of("e1", "e2", "e3"));

    var subject = generate(true);

    assertEquals(7, subject.avoidedIdsOfExtraSearches().size());
    assertThat(subject.avoidedIdsOfExtraSearches()).containsNoDuplicates();
  }

  @Test
  void aPathWithFourClosableFacilityIdsNeedsOneSearchForEachOfTheFifteenSubsets() {
    provider.addNearby(A, B, 100, 60, Set.of("e1", "e2", "e3", "e4"));

    var subject = generate(true);

    assertEquals(15, subject.avoidedIdsOfExtraSearches().size());
    assertThat(subject.avoidedIdsOfExtraSearches()).containsNoDuplicates();
  }

  @Test
  void aTransferWithoutAPathThatAvoidsTheIdHasNoAlternative() {
    var best = provider.addNearby(A, B, 100, 60, Set.of("e1"));

    var subject = generate(true);

    assertThat(subject.paths(FIRST, B_INDEX)).containsExactly(alternative(best, "e1"));
    assertThat(subject.avoidedIdsOfExtraSearches()).containsExactly(Set.of("e1"));
  }

  @Test
  void anAlternativeFoundForSeveralSubsetsIsStoredOnce() {
    provider.addNearby(A, B, 100, 60, Set.of("e1", "e2"));
    var avoidsBoth = provider.addNearby(A, B, 400, 60);

    var subject = generate(true);

    assertThat(subject.paths(FIRST, B_INDEX).stream().map(AlternativePath::path)).contains(
      avoidsBoth
    );
    assertEquals(2, subject.paths(FIRST, B_INDEX).size());
  }

  @Test
  void theTransfersFromAStopThatNeedTheSameSubsetShareTheSearch() {
    provider.addNearby(A, B, 100, 60, Set.of("e1"));
    provider.addNearby(A, C, 100, 60, Set.of("e1"));
    provider.addNearby(A, C, 300, 60);

    var subject = generate(true);

    assertThat(subject.avoidedIdsOfExtraSearches()).containsExactly(Set.of("e1"));
    assertEquals(1, subject.paths(FIRST, B_INDEX).size());
    assertEquals(2, subject.paths(FIRST, C_INDEX).size());
  }

  @Test
  void aTransferWithoutClosableFacilityIdsHasNoAlternativePaths() {
    provider.addNearby(A, B, 100, 60);

    var subject = generate(true);

    assertThat(subject.paths(FIRST, B_INDEX)).isEmpty();
    assertThat(subject.avoidedIdsOfExtraSearches()).isEmpty();
  }

  @Test
  void noAlternativesAreGeneratedByDefault() {
    provider.addNearby(A, B, 100, 60, Set.of("e1"));
    provider.addNearby(A, B, 300, 60);

    var subject = generate(false);

    assertThat(subject.paths(FIRST, B_INDEX)).isEmpty();
    assertThat(subject.avoidedIdsOfExtraSearches()).isEmpty();
    // One search for each stop and profile
    assertEquals(3, provider.avoidedIdsOfSearches().size());
  }

  @Test
  void aProfileReusesThePathOfAnEarlierProfileIfItHasTheSameClosableFacilityIds() {
    var p1 = provider.addNearby(A, B, 100, 60, Set.of("e1"));
    var p2 = provider.addNearby(A, B, 100, 61, Set.of("e1"));
    provider.withCost(p1, 2.0, 210).withCost(p2, 2.0, 200);

    var subject = generate(true, FIRST, SECOND);

    assertThat(subject.bestPath(SECOND, B_INDEX)).isSameInstanceAs(p1);
  }

  @Test
  void aProfileDoesNotReuseThePathOfAnEarlierProfileThatHasOtherClosableFacilityIds() {
    var p1 = provider.addNearby(A, B, 100, 60, Set.of("e1"));
    var p2 = provider.addNearby(A, B, 100, 61, Set.of("e2"));
    provider.withCost(p1, 2.0, 210).withCost(p2, 2.0, 200);

    var subject = generate(true, FIRST, SECOND);

    assertThat(subject.bestPath(FIRST, B_INDEX)).isSameInstanceAs(p1);
    assertThat(subject.bestPath(SECOND, B_INDEX)).isSameInstanceAs(p2);
  }

  @Test
  void theClosableFacilitiesAreIgnoredWhenTheFeatureIsOff() {
    var p1 = provider.addNearby(A, B, 100, 60, Set.of("e1"));
    var p2 = provider.addNearby(A, B, 100, 61, Set.of("e2"));
    provider.withCost(p1, 2.0, 210).withCost(p2, 2.0, 200);

    var subject = generate(false, FIRST, SECOND);

    assertThat(subject.bestPath(SECOND, B_INDEX)).isSameInstanceAs(p1);
  }

  @Test
  void theAlternativePathsAreSerializedWithTheBuildRepository() throws Exception {
    var repository = new DefaultRegularTransferBuildRepository<String>();
    repository.setPaths(FIRST, A_INDEX, Map.of(B_INDEX, "best"));
    var paths = List.of(
      new AlternativePath<>("best", Set.of("e1")),
      new AlternativePath<>("detour", Set.<String>of())
    );
    repository.setAlternativePaths(FIRST, A_INDEX, Map.of(B_INDEX, paths));

    var bytes = new ByteArrayOutputStream();
    try (var out = new ObjectOutputStream(bytes)) {
      out.writeObject(repository);
    }
    try (var in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
      @SuppressWarnings("unchecked")
      var copy = (DefaultRegularTransferBuildRepository<String>) in.readObject();
      var snapshot = (DefaultRegularTransferRepositorySnapshot<
        String,
        WalkPreferences
      >) TestLifecycles.initialSnapshot(copy);
      assertEquals(paths, snapshot.paths().alternativePaths(FIRST, A_INDEX, B_INDEX));
    }
  }

  private Generated generate(boolean pathAlternatives, WalkPreferences... profiles) {
    var walkProfiles = (profiles.length == 0 ? List.of(FIRST) : List.of(profiles))
      .stream()
      .map(p -> new TransferProfile<>("walk-" + p.reluctance(), TransferProfileType.WALK, p, p))
      .toList();
    var buildRepository = new DefaultRegularTransferBuildRepository<FakePath>();
    new DefaultRegularTransferGenerator<>(
      TestStopIndex.of(A, B, C),
      new TransferParameters<>(
        walkProfiles,
        Map.of(TransferProfileType.WALK, MaxDurationsPreference.DEFAULT),
        List.of(),
        pathAlternatives
      ),
      provider,
      FakeRequestMapper.INSTANCE,
      buildRepository
    ).generateTransfersForAllStops();
    var snapshot = (DefaultRegularTransferRepositorySnapshot<
      FakePath,
      WalkPreferences
    >) TestLifecycles.initialSnapshot(buildRepository);
    return new Generated(snapshot.paths(), provider);
  }

  private static AlternativePath<FakePath> alternative(FakePath path, String... ids) {
    return new AlternativePath<>(path, Set.of(ids));
  }

  private static WalkPreferences withReluctance(double reluctance) {
    return WalkPreferences.of().withReluctance(Reluctance.of(reluctance)).build();
  }

  /** The paths generated from stop A. */
  private record Generated(
    OverlaidTemplatePaths<FakePath> paths,
    FakeTransferPathProvider provider
  ) {
    List<AlternativePath<FakePath>> paths(WalkPreferences profile, int toStop) {
      return paths.alternativePaths(profile, A_INDEX, toStop);
    }

    FakePath bestPath(WalkPreferences profile, int toStop) {
      return paths.findPath(profile, A_INDEX, toStop);
    }

    /** The ids avoided by the searches that avoid something, in the order of the searches. */
    List<Set<String>> avoidedIdsOfExtraSearches() {
      return provider
        .avoidedIdsOfSearches()
        .stream()
        .filter(ids -> !ids.isEmpty())
        .toList();
    }
  }
}
