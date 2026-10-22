package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.api.WalkPreferences;

/**
 * The write side of the runtime repository: replacing and resetting the transfers from a stop,
 * on top of the paths generated at graph build, and tracking the changed stops.
 */
class DefaultRegularTransferRepositoryTest {

  private static final WalkPreferences WALK = WalkPreferences.DEFAULT;
  private static final WalkPreferences UNKNOWN = withReluctance(7.0);

  private static final int A = 0;
  private static final int B = 1;
  private static final int C = 2;
  private static final int D = 3;

  private static final String PATH_AB = "A-B";
  private static final String PATH_AC = "A-C";
  private static final String PATH_BC = "B-C";
  private static final String PATH_AB_NEW = "A-B (new)";
  private static final String PATH_AD = "A-D";
  private static final String PATH_CA = "C-A";

  private DefaultRegularTransferBuildRepository<String> buildRepository;
  private DefaultRegularTransferRepository<String> repository;
  private RegularTransferRepositoryLifecycle<String> lifecycle;

  /** Graph build: A -> B, A -> C, B -> C */
  @BeforeEach
  void setUp() {
    buildRepository = new DefaultRegularTransferBuildRepository<>();
    buildRepository.setPaths(WALK, A, Map.of(B, PATH_AB, C, PATH_AC));
    buildRepository.setPaths(WALK, B, Map.of(C, PATH_BC));
    repository =
      (DefaultRegularTransferRepository<String>) buildRepository.createRuntimeRepository();
    lifecycle = TestLifecycles.pathsOnly(repository);
  }

  @Test
  void withoutWritesTheSnapshotHasTheGraphBuildPaths() {
    var snapshot = freeze();
    assertThat(snapshot.findPath(WALK, A, B)).isEqualTo(PATH_AB);
    assertThat(snapshot.findPath(WALK, A, C)).isEqualTo(PATH_AC);
    assertThat(snapshot.findPath(WALK, B, C)).isEqualTo(PATH_BC);
  }

  @Test
  void replaceHidesAllGraphBuildPathsFromTheStop() {
    repository.replaceTransfersFrom(WALK, A, Map.of(B, PATH_AB_NEW, D, PATH_AD));

    var snapshot = freeze();
    assertThat(snapshot.findPath(WALK, A, B)).isEqualTo(PATH_AB_NEW);
    assertThat(snapshot.findPath(WALK, A, D)).isEqualTo(PATH_AD);
    // A -> C is not in the new paths, so it is gone - replace does not merge
    assertThat(snapshot.findPath(WALK, A, C)).isNull();
    // Other stops keep their graph-build paths
    assertThat(snapshot.findPath(WALK, B, C)).isEqualTo(PATH_BC);
  }

  @Test
  void replaceWithNoPathsRemovesAllTransfersFromTheStop() {
    repository.replaceTransfersFrom(WALK, A, Map.of());

    var snapshot = freeze();
    assertThat(snapshot.findPath(WALK, A, B)).isNull();
    assertThat(snapshot.findPath(WALK, A, C)).isNull();
    assertThat(snapshot.findPath(WALK, B, C)).isEqualTo(PATH_BC);
  }

  @Test
  void replaceAddsTransfersFromAStopWithoutGraphBuildPaths() {
    repository.replaceTransfersFrom(WALK, C, Map.of(A, PATH_CA));

    assertThat(freeze().findPath(WALK, C, A)).isEqualTo(PATH_CA);
  }

  @Test
  void theLastReplaceWins() {
    repository.replaceTransfersFrom(WALK, A, Map.of(D, PATH_AD));
    repository.replaceTransfersFrom(WALK, A, Map.of(B, PATH_AB_NEW));

    var snapshot = freeze();
    assertThat(snapshot.findPath(WALK, A, B)).isEqualTo(PATH_AB_NEW);
    assertThat(snapshot.findPath(WALK, A, D)).isNull();
  }

  @Test
  void replaceDoesNotRetainTheGivenMap() {
    var paths = new HashMap<Integer, String>();
    paths.put(B, PATH_AB_NEW);

    repository.replaceTransfersFrom(WALK, A, paths);
    paths.put(D, PATH_AD);

    assertThat(freeze().findPath(WALK, A, D)).isNull();
  }

  @Test
  void resetRestoresTheGraphBuildPaths() {
    repository.replaceTransfersFrom(WALK, A, Map.of(D, PATH_AD));
    freeze();

    repository.resetTransfersFrom(WALK, A);

    var snapshot = freeze();
    assertThat(snapshot.findPath(WALK, A, B)).isEqualTo(PATH_AB);
    assertThat(snapshot.findPath(WALK, A, C)).isEqualTo(PATH_AC);
    assertThat(snapshot.findPath(WALK, A, D)).isNull();
  }

  @Test
  void resetOfAStopWithoutGraphBuildPathsRemovesItsTransfers() {
    repository.replaceTransfersFrom(WALK, C, Map.of(A, PATH_CA));
    repository.resetTransfersFrom(WALK, C);

    assertThat(freeze().findPath(WALK, C, A)).isNull();
  }

  @Test
  void writesToAnUnknownProfileAreRejected() {
    assertThrows(IllegalArgumentException.class, () ->
      repository.replaceTransfersFrom(UNKNOWN, A, Map.of(B, PATH_AB_NEW))
    );
    assertThrows(IllegalArgumentException.class, () -> repository.resetTransfersFrom(UNKNOWN, A));
    assertThat(repository.hasChanges()).isFalse();
  }

  @Test
  void theChangedStopsAreTrackedByProfileUntilFreeze() {
    repository.replaceTransfersFrom(WALK, A, Map.of(D, PATH_AD));
    repository.replaceTransfersFrom(WALK, C, Map.of());
    repository.replaceTransfersFrom(WALK, A, Map.of(B, PATH_AB_NEW));

    assertThat(repository.hasChanges()).isTrue();
    assertThat(repository.freeze().changedFromStops()).containsExactly(WALK, Set.of(A, C));

    // A freeze starts the tracking anew
    assertThat(repository.hasChanges()).isFalse();
    assertThat(repository.freeze().changedFromStops()).isEmpty();
  }

  @Test
  void aResetIsAChangeOnlyIfTheStopWasReplaced() {
    repository.resetTransfersFrom(WALK, A);
    assertThat(repository.hasChanges()).isFalse();

    repository.replaceTransfersFrom(WALK, A, Map.of());
    repository.freeze();
    repository.resetTransfersFrom(WALK, A);
    assertThat(repository.freeze().changedFromStops()).containsExactly(WALK, Set.of(A));
  }

  @Test
  void writesAfterFreezeDoNotChangeThePublishedSnapshot() {
    repository.replaceTransfersFrom(WALK, A, Map.of(D, PATH_AD));
    var first = freeze();

    repository.replaceTransfersFrom(WALK, A, Map.of(B, PATH_AB_NEW));
    repository.replaceTransfersFrom(WALK, C, Map.of(A, PATH_CA));
    var second = freeze();

    assertThat(first.findPath(WALK, A, D)).isEqualTo(PATH_AD);
    assertThat(first.findPath(WALK, A, B)).isNull();
    assertThat(first.findPath(WALK, C, A)).isNull();
    assertThat(second.findPath(WALK, A, B)).isEqualTo(PATH_AB_NEW);
    assertThat(second.findPath(WALK, C, A)).isEqualTo(PATH_CA);
  }

  @Test
  void writesDoNotChangeTheBuildRepository() {
    repository.replaceTransfersFrom(WALK, A, Map.of(D, PATH_AD));
    repository.replaceTransfersFrom(WALK, B, Map.of());

    // A new runtime repository starts from the unchanged paths of the build repository
    var snapshot = TestLifecycles.initialSnapshot(buildRepository);
    assertThat(snapshot.findPath(WALK, A, B)).isEqualTo(PATH_AB);
    assertThat(snapshot.findPath(WALK, A, C)).isEqualTo(PATH_AC);
    assertThat(snapshot.findPath(WALK, A, D)).isNull();
    assertThat(snapshot.findPath(WALK, B, C)).isEqualTo(PATH_BC);
  }

  @Test
  void discardChangesContinuesFromTheCommittedPaths() {
    repository.replaceTransfersFrom(WALK, A, Map.of(D, PATH_AD));
    var committed = (DefaultRegularTransferRepositorySnapshot<String>) freeze();

    repository.replaceTransfersFrom(WALK, A, Map.of(B, PATH_AB_NEW));
    repository.replaceTransfersFrom(WALK, C, Map.of(A, PATH_CA));
    repository.discardChanges(committed.paths());

    assertThat(repository.hasChanges()).isFalse();
    var snapshot = freeze();
    assertThat(snapshot.findPath(WALK, A, D)).isEqualTo(PATH_AD);
    assertThat(snapshot.findPath(WALK, A, B)).isNull();
    assertThat(snapshot.findPath(WALK, C, A)).isNull();
  }

  @Test
  void writesAreMadeToTheExactProfileAndReadUnderTheClosestOne() {
    var low = withReluctance(1.0);
    var high = withReluctance(4.0);
    var build = new DefaultRegularTransferBuildRepository<String>();
    build.setPaths(low, A, Map.of(B, PATH_AB));
    build.setPaths(high, A, Map.of(B, PATH_AB));
    var repository = build.createRuntimeRepository();
    var lifecycle = TestLifecycles.pathsOnly(repository);

    repository.replaceTransfersFrom(high, A, Map.of(B, PATH_AB_NEW));

    var snapshot = lifecycle.freeze(repository);
    assertThat(snapshot.findPath(withReluctance(1.5), A, B)).isEqualTo(PATH_AB);
    assertThat(snapshot.findPath(withReluctance(3.5), A, B)).isEqualTo(PATH_AB_NEW);
    // Close is not exact: a write must name the profile itself
    assertThrows(IllegalArgumentException.class, () ->
      repository.replaceTransfersFrom(withReluctance(3.5), A, Map.of())
    );
  }

  private RegularTransferRepositorySnapshot<String> freeze() {
    return lifecycle.freeze(repository);
  }

  private static WalkPreferences withReluctance(double reluctance) {
    return WalkPreferences.of().withReluctance(Reluctance.of(reluctance)).build();
  }
}
