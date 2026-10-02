package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    var data = new DefaultRegularTransferBuildRepository<String>();
    assertThat(data.hasTransfersFrom(0)).isFalse();
  }

  @Test
  void hasTransfersFromIsTrueOnceAPathIsStoredForAnyProfile() {
    var data = new DefaultRegularTransferBuildRepository<String>();
    data.setPaths(WALK, 0, Map.of(1, "A -> B"));

    assertThat(data.hasTransfersFrom(0)).isTrue();
    // Only the fromStop that was actually stored to is linked.
    assertThat(data.hasTransfersFrom(1)).isFalse();
  }

  @Test
  void calculateNumberOfTransferPathsCountsAllStoredPaths() {
    var data = createData();

    assertThat(data.calculateNumberOfTransferPaths()).isEqualTo(3);
    assertThat(data.pathsFor(WALK)).hasSize(3);
  }

  @Test
  void setPathsKeepsThePathsToOtherStops() {
    var data = createData();

    data.setPaths(WALK, A, Map.of(B, PATH_AB_NEW));

    assertThat(data.pathsFor(WALK)).containsExactly(
      new StoredPath<>(A, B, PATH_AB_NEW),
      new StoredPath<>(A, C, PATH_AC),
      new StoredPath<>(B, C, PATH_BC)
    );
  }

  @Test
  void initialSnapshotContainsTheGeneratedPaths() {
    var snapshot = createData().createInitialSnapshot();

    assertThat(snapshot.findPath(WALK, A, B)).isEqualTo(PATH_AB);
    assertThat(snapshot.pathsFor(WALK)).hasSize(3);
  }

  @Test
  void theInitialSnapshotSealsTheData() {
    var data = createData();

    data.createInitialSnapshot();

    var ex = assertThrows(IllegalStateException.class, () ->
      data.setPaths(WALK, A, Map.of(B, PATH_AB_NEW))
    );
    assertThat(ex).hasMessageThat().contains("sealed");
    // Reads still work
    assertThat(data.hasTransfersFrom(A)).isTrue();
    assertThat(data.calculateNumberOfTransferPaths()).isEqualTo(3);
  }

  @Test
  void repositoryWritesDoNotChangeTheData() {
    var data = createData();
    var repository = lifecycle.copyOnWrite(data.createInitialSnapshot());

    // A replaces the shared toStop map of stop A in the repository only
    repository.setPath(WALK, A, B, PATH_AB_NEW);

    assertThat(data.pathsFor(WALK)).contains(new StoredPath<>(A, B, PATH_AB));
    assertThat(lifecycle.freeze(repository).findPath(WALK, A, B)).isEqualTo(PATH_AB_NEW);
  }

  private static DefaultRegularTransferBuildRepository<String> createData() {
    var data = new DefaultRegularTransferBuildRepository<String>();
    data.setPaths(WALK, A, Map.of(B, PATH_AB, C, PATH_AC));
    data.setPaths(WALK, B, Map.of(C, PATH_BC));
    return data;
  }
}
