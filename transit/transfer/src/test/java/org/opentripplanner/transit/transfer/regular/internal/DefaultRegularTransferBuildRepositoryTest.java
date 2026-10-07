package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.transit.transfer.regular.parameters.TransferProfileType.WALK;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot.StoredPath;

class DefaultRegularTransferBuildRepositoryTest {

  private static final int A = 0;
  private static final int B = 1;
  private static final int C = 2;

  private static final String PATH_AB = "A-B";
  private static final String PATH_AC = "A-C";
  private static final String PATH_BC = "B-C";
  private static final String PATH_AB_NEW = "A-B (new)";

  private final RegularTransferRepositoryLifecycle<String> lifecycle =
    new RegularTransferRepositoryLifecycle<>();

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
  void createInitialSnapshotContainsTheGeneratedPaths() {
    var snapshot = createBuildRepository().createInitialSnapshot();

    assertThat(snapshot.findPath(WALK, A, B)).isEqualTo(PATH_AB);
    assertThat(snapshot.pathsFor(WALK)).hasSize(3);
  }

  @Test
  void runtimeWritesDoNotChangeTheBuildRepository() {
    var buildRepository = createBuildRepository();
    var runtimeRepository = lifecycle.copyOnWrite(buildRepository.createInitialSnapshot());

    // Replaces the toStop map of stop A, shared with the build repository, in the runtime
    // repository only
    runtimeRepository.setPath(WALK, A, B, PATH_AB_NEW);

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
