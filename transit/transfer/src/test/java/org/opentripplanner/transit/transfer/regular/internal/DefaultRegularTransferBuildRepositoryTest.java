package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot.StoredPath;
import org.opentripplanner.transit.transfer.regular.api.WalkPreferences;

class DefaultRegularTransferBuildRepositoryTest {

  private static final WalkPreferences WALK = WalkPreferences.DEFAULT;

  private static final int A = 0;
  private static final int B = 1;
  private static final int C = 2;

  private static final String PATH_AB = "A-B";
  private static final String PATH_AC = "A-C";
  private static final String PATH_BC = "B-C";
  private static final String PATH_AB_NEW = "A-B (new)";

  @Test
  void hasTransfersFromIsFalseForAStopWithNoStoredPaths() {
    var buildRepository = new DefaultRegularTransferBuildRepository<String>();
    assertThat(buildRepository.hasTransfersFrom(0)).isFalse();
  }

  @Test
  void hasTransfersFromIsTrueOnceAPathIsStoredForAnyProfile() {
    var buildRepository = new DefaultRegularTransferBuildRepository<String>();
    buildRepository.setPaths(WALK, 0, Map.of(1, "A -> B"));

    assertThat(buildRepository.hasTransfersFrom(0)).isTrue();
    // Only the fromStop that was actually stored to is linked.
    assertThat(buildRepository.hasTransfersFrom(1)).isFalse();
  }

  @Test
  void calculateNumberOfTransferPathsCountsAllStoredPaths() {
    var buildRepository = createBuildRepository();

    assertThat(buildRepository.calculateNumberOfTransferPaths()).isEqualTo(3);
    assertThat(buildRepository.calculateNumberOfTransferPaths(WALK)).isEqualTo(3);
    assertThat(buildRepository.pathsFor(WALK)).hasSize(3);
  }

  @Test
  void setPathsKeepsThePathsToOtherStops() {
    var buildRepository = createBuildRepository();

    buildRepository.setPaths(WALK, A, Map.of(B, PATH_AB_NEW));

    assertThat(buildRepository.pathsFor(WALK)).containsExactly(
      new StoredPath<>(A, B, PATH_AB_NEW),
      new StoredPath<>(A, C, PATH_AC),
      new StoredPath<>(B, C, PATH_BC)
    );
  }

  @Test
  void runtimeRepositoryContainsTheGeneratedPaths() {
    var snapshot = TestLifecycles.initialSnapshot(createBuildRepository());

    assertThat(snapshot.findPath(WALK, A, B)).isEqualTo(PATH_AB);
    assertThat(snapshot.pathsFor(WALK)).hasSize(3);
  }

  @Test
  void createRuntimeRepositorySealsTheBuildRepository() {
    var buildRepository = createBuildRepository();

    buildRepository.createRuntimeRepository();

    var ex = assertThrows(IllegalStateException.class, () ->
      buildRepository.setPaths(WALK, A, Map.of(B, PATH_AB_NEW))
    );
    assertThat(ex).hasMessageThat().contains("sealed");
    // Reads still work
    assertThat(buildRepository.hasTransfersFrom(A)).isTrue();
    assertThat(buildRepository.calculateNumberOfTransferPaths()).isEqualTo(3);
  }

  @Test
  void runtimeWritesDoNotChangeTheBuildRepository() {
    var buildRepository = createBuildRepository();
    var runtimeRepository = buildRepository.createRuntimeRepository();
    var lifecycle = TestLifecycles.pathsOnly(runtimeRepository);

    // The paths of the build repository are the base of the runtime repository, a write
    // replaces the paths from stop A in the runtime repository only
    runtimeRepository.replaceTransfersFrom(WALK, A, Map.of(B, PATH_AB_NEW));

    assertThat(buildRepository.pathsFor(WALK)).contains(new StoredPath<>(A, B, PATH_AB));
    assertThat(lifecycle.freeze(runtimeRepository).findPath(WALK, A, B)).isEqualTo(PATH_AB_NEW);
  }

  private static DefaultRegularTransferBuildRepository<String> createBuildRepository() {
    var buildRepository = new DefaultRegularTransferBuildRepository<String>();
    buildRepository.setPaths(WALK, A, Map.of(B, PATH_AB, C, PATH_AC));
    buildRepository.setPaths(WALK, B, Map.of(C, PATH_BC));
    return buildRepository;
  }
}
