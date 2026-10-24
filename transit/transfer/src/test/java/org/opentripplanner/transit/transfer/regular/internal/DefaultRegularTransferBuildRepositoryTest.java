package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Reluctance;
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
  void calculateNumberOfTransferPathsCountsThePathsOfTheGivenPreferencesOnly() {
    var buildRepository = createBuildRepository();
    var other = WalkPreferences.of().withReluctance(Reluctance.of(4.0)).build();
    buildRepository.setPaths(other, A, Map.of(B, PATH_AB));

    assertThat(buildRepository.calculateNumberOfTransferPaths(WALK)).isEqualTo(3);
    assertThat(buildRepository.calculateNumberOfTransferPaths(other)).isEqualTo(1);
  }

  @Test
  void setPathsKeepsThePathsToOtherStops() {
    var buildRepository = createBuildRepository();

    buildRepository.setPaths(WALK, A, Map.of(B, PATH_AB_NEW));

    var snapshot = TestLifecycles.initialSnapshot(buildRepository);
    assertThat(snapshot.forRequest(WALK).findPath(A, B)).isEqualTo(PATH_AB_NEW);
    assertThat(snapshot.forRequest(WALK).findPath(A, C)).isEqualTo(PATH_AC);
    assertThat(snapshot.forRequest(WALK).findPath(B, C)).isEqualTo(PATH_BC);
  }

  @Test
  void runtimeRepositoryContainsTheGeneratedPaths() {
    var snapshot = TestLifecycles.initialSnapshot(createBuildRepository());

    assertThat(snapshot.forRequest(WALK).findPath(A, B)).isEqualTo(PATH_AB);
    assertThat(snapshot.forRequest(WALK).findPath(A, C)).isEqualTo(PATH_AC);
    assertThat(snapshot.forRequest(WALK).findPath(B, C)).isEqualTo(PATH_BC);
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
    assertThat(buildRepository.calculateNumberOfTransferPaths(WALK)).isEqualTo(3);
  }

  @Test
  void runtimeWritesDoNotChangeTheBuildRepository() {
    var buildRepository = createBuildRepository();
    var runtimeRepository = buildRepository.createRuntimeRepository();
    var lifecycle = TestLifecycles.<String>pathsOnly();

    // The paths of the build repository are the base of the runtime repository, a write
    // replaces the paths from stop A in the runtime repository only
    runtimeRepository.replaceTransfersFrom(WALK, A, Map.of(B, PATH_AB_NEW));

    assertThat(lifecycle.freeze(runtimeRepository).forRequest(WALK).findPath(A, B)).isEqualTo(
      PATH_AB_NEW
    );
    // A new runtime repository starts from the unchanged paths of the build repository
    assertThat(
      TestLifecycles.initialSnapshot(buildRepository).forRequest(WALK).findPath(A, B)
    ).isEqualTo(PATH_AB);
  }

  private static DefaultRegularTransferBuildRepository<String> createBuildRepository() {
    var buildRepository = new DefaultRegularTransferBuildRepository<String>();
    buildRepository.setPaths(WALK, A, Map.of(B, PATH_AB, C, PATH_AC));
    buildRepository.setPaths(WALK, B, Map.of(C, PATH_BC));
    return buildRepository;
  }
}
