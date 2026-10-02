package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.transit.transfer.regular.parameters.TransferProfileType.WALK;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot.StoredPath;

class DefaultRegularTransferRepositoryTest {

  private static final int A = 0;
  private static final int B = 1;
  private static final int C = 2;

  private static final String PATH_AB = "A-B";
  private static final String PATH_AC = "A-C";
  private static final String PATH_BC = "B-C";
  private static final String PATH_AB_NEW = "A-B (new)";
  private static final String PATH_CA = "C-A";

  @Test
  void emptyRepository() {
    var snapshot = new DefaultRegularTransferRepository<String>().freeze();

    assertThat(snapshot.findPath(WALK, A, B)).isNull();
    assertThat(snapshot.pathsFor(WALK)).isEmpty();
  }

  @Test
  void freezePublishesTheWrittenPaths() {
    var snapshot = createSnapshot();

    assertThat(snapshot.findPath(WALK, A, B)).isEqualTo(PATH_AB);
    assertThat(snapshot.findPath(WALK, B, A)).isNull();
    assertThat(snapshot.pathsFor(WALK)).containsExactly(
      new StoredPath<>(A, B, PATH_AB),
      new StoredPath<>(A, C, PATH_AC),
      new StoredPath<>(B, C, PATH_BC)
    );
  }

  @Test
  void setPathsKeepsThePathsToOtherStops() {
    var repository = createSnapshot().copyOnWrite();

    repository.setPaths(WALK, A, Map.of(B, PATH_AB_NEW));

    var snapshot = repository.freeze();
    assertThat(snapshot.findPath(WALK, A, B)).isEqualTo(PATH_AB_NEW);
    assertThat(snapshot.findPath(WALK, A, C)).isEqualTo(PATH_AC);
  }

  @Test
  void setPathsOnACopyOnWriteRepositoryDoesNotChangeTheSnapshot() {
    var original = createSnapshot();
    var repository = original.copyOnWrite();

    repository.setPaths(WALK, A, Map.of(B, PATH_AB_NEW));
    repository.setPaths(WALK, C, Map.of(A, PATH_CA));

    assertThat(original.findPath(WALK, A, B)).isEqualTo(PATH_AB);
    assertThat(original.findPath(WALK, C, A)).isNull();
  }

  @Test
  void setPathsWithNoPathsStoresNothing() {
    var repository = new DefaultRegularTransferRepository<String>();

    repository.setPaths(WALK, A, Map.of());

    assertThat(repository.freeze().pathsFor(WALK)).isEmpty();
  }

  @Test
  void setPathsDoesNotRetainTheGivenMap() {
    var repository = new DefaultRegularTransferRepository<String>();
    var paths = new HashMap<Integer, String>();
    paths.put(B, PATH_AB);

    repository.setPaths(WALK, A, paths);
    paths.put(C, PATH_AC);

    assertThat(repository.freeze().findPath(WALK, A, C)).isNull();
  }

  @Test
  void writesToACopyOnWriteRepositoryDoNotChangeTheSnapshot() {
    var original = createSnapshot();
    var repository = original.copyOnWrite();

    // Replace a path for an existing fromStop, and add a path for a new fromStop
    repository.setPath(WALK, A, B, PATH_AB_NEW);
    repository.setPath(WALK, C, A, PATH_CA);
    var updated = repository.freeze();

    assertThat(original.findPath(WALK, A, B)).isEqualTo(PATH_AB);
    assertThat(original.findPath(WALK, C, A)).isNull();
    assertThat(original.pathsFor(WALK)).hasSize(3);

    assertThat(updated.findPath(WALK, A, B)).isEqualTo(PATH_AB_NEW);
    assertThat(updated.findPath(WALK, C, A)).isEqualTo(PATH_CA);
    // Untouched paths are carried over
    assertThat(updated.findPath(WALK, A, C)).isEqualTo(PATH_AC);
    assertThat(updated.findPath(WALK, B, C)).isEqualTo(PATH_BC);
  }

  @Test
  void writesForAProfileMissingInTheSnapshotDoNotChangeTheSnapshot() {
    var original = new DefaultRegularTransferRepository<String>().freeze();
    var repository = original.copyOnWrite();

    repository.setPath(WALK, A, B, PATH_AB);

    assertThat(original.pathsFor(WALK)).isEmpty();
    assertThat(repository.freeze().findPath(WALK, A, B)).isEqualTo(PATH_AB);
  }

  @Test
  void writesAfterFreezeDoNotChangeThePublishedSnapshot() {
    var repository = new DefaultRegularTransferRepository<String>();
    repository.setPath(WALK, A, B, PATH_AB);
    var first = repository.freeze();

    repository.setPath(WALK, A, B, PATH_AB_NEW);
    repository.setPath(WALK, A, C, PATH_AC);
    var second = repository.freeze();

    assertThat(first.pathsFor(WALK)).containsExactly(new StoredPath<>(A, B, PATH_AB));
    assertThat(second.pathsFor(WALK)).containsExactly(
      new StoredPath<>(A, B, PATH_AB_NEW),
      new StoredPath<>(A, C, PATH_AC)
    );
  }

  @Test
  void successiveCopyOnWriteRepositoriesAreIndependent() {
    var original = createSnapshot();

    var first = original.copyOnWrite();
    first.setPath(WALK, A, B, PATH_AB_NEW);
    var firstSnapshot = first.freeze();

    var second = firstSnapshot.copyOnWrite();
    second.setPath(WALK, A, C, PATH_CA);
    var secondSnapshot = second.freeze();

    assertThat(original.findPath(WALK, A, B)).isEqualTo(PATH_AB);
    assertThat(original.findPath(WALK, A, C)).isEqualTo(PATH_AC);
    assertThat(firstSnapshot.findPath(WALK, A, B)).isEqualTo(PATH_AB_NEW);
    assertThat(firstSnapshot.findPath(WALK, A, C)).isEqualTo(PATH_AC);
    assertThat(secondSnapshot.findPath(WALK, A, B)).isEqualTo(PATH_AB_NEW);
    assertThat(secondSnapshot.findPath(WALK, A, C)).isEqualTo(PATH_CA);
  }

  private static DefaultRegularTransferRepositorySnapshot<String> createSnapshot() {
    var repository = new DefaultRegularTransferRepository<String>();
    repository.setPath(WALK, A, B, PATH_AB);
    repository.setPath(WALK, A, C, PATH_AC);
    repository.setPath(WALK, B, C, PATH_BC);
    return repository.freeze();
  }
}
