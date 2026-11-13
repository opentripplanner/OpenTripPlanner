package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.transfer.regular.AlternativePath;
import org.opentripplanner.transit.transfer.regular.api.preferences.WalkPreferences;

/**
 * Closing facilities in the write side of the runtime repository: the complete set of closed
 * facilities is given every time, and the stops that have a path that depends on a facility that
 * was closed or opened are tracked as changed.
 */
class DefaultRegularTransferRepositoryCloseFacilitiesTest {

  private static final WalkPreferences WALK = WalkPreferences.DEFAULT;

  private static final int A = 0;
  private static final int B = 1;
  private static final int C = 2;
  private static final int D = 3;

  private static final String E1 = "e1";
  private static final String E2 = "e2";
  private static final String UNKNOWN = "unknown";

  private DefaultRegularTransferBuildRepository<String> buildRepository;
  private DefaultRegularTransferRepository<String> repository;
  private RegularTransferRepositoryLifecycle<String, WalkPreferences> lifecycle;

  /**
   * Graph build: A -> B depends on e1, A -> C depends on e1 and e2, B -> C depends on e2, and
   * C -> D depends on nothing.
   */
  @BeforeEach
  void setUp() {
    buildRepository = new DefaultRegularTransferBuildRepository<>();
    buildRepository.setPaths(WALK, A, Map.of(B, "A-B", C, "A-C"));
    buildRepository.setPaths(WALK, B, Map.of(C, "B-C"));
    buildRepository.setPaths(WALK, C, Map.of(D, "C-D"));
    buildRepository.setAlternativePaths(
      WALK,
      A,
      Map.of(
        B,
        List.of(new AlternativePath<>("A-B", Set.of(E1)), new AlternativePath<>("A-B'", Set.of())),
        C,
        List.of(
          new AlternativePath<>("A-C", Set.of(E1, E2)),
          new AlternativePath<>("A-C'", Set.of())
        )
      )
    );
    buildRepository.setAlternativePaths(
      WALK,
      B,
      Map.of(
        C,
        List.of(new AlternativePath<>("B-C", Set.of(E2)), new AlternativePath<>("B-C'", Set.of()))
      )
    );
    repository =
      (DefaultRegularTransferRepository<String>) buildRepository.createRuntimeRepository();
    lifecycle = TestLifecycles.pathsOnly();
  }

  @Test
  void noFacilityIsClosedAtStart() {
    assertThat(repository.freeze().closedFacilityIds()).isEmpty();
  }

  @Test
  void theStopsWithAPathThatDependsOnAClosedFacilityAreChanged() {
    repository.closeFacilities(Set.of(E1));

    var frozen = repository.freeze();
    assertThat(frozen.closedFacilityIds()).containsExactly(E1);
    assertThat(frozen.changedFromStops()).containsExactly(WALK, Set.of(A));
  }

  @Test
  void aFacilityThatSeveralStopsDependOnChangesAllOfThem() {
    repository.closeFacilities(Set.of(E2));

    assertThat(repository.freeze().changedFromStops()).containsExactly(WALK, Set.of(A, B));
  }

  @Test
  void theSameSetTwiceChangesNothing() {
    repository.closeFacilities(Set.of(E1));
    publishAndContinue();

    repository.closeFacilities(Set.of(E1));

    var frozen = repository.freeze();
    assertThat(frozen.closedFacilityIds()).containsExactly(E1);
    assertThat(frozen.changedFromStops()).isEmpty();
  }

  @Test
  void onlyTheFacilitiesThatChangedChangeStops() {
    repository.closeFacilities(Set.of(E1));
    publishAndContinue();

    // e1 stays closed, e2 is closed
    repository.closeFacilities(Set.of(E1, E2));

    assertThat(repository.freeze().changedFromStops()).containsExactly(WALK, Set.of(A, B));
  }

  @Test
  void anEmptySetOpensAllFacilities() {
    repository.closeFacilities(Set.of(E1, E2));
    publishAndContinue();

    repository.closeFacilities(Set.of());

    var frozen = repository.freeze();
    assertThat(frozen.closedFacilityIds()).isEmpty();
    assertThat(frozen.changedFromStops()).containsExactly(WALK, Set.of(A, B));
  }

  @Test
  void aFacilityNoPathDependsOnIsKeptAndChangesNothing() {
    repository.closeFacilities(Set.of(UNKNOWN));

    var frozen = repository.freeze();
    assertThat(frozen.closedFacilityIds()).containsExactly(UNKNOWN);
    assertThat(frozen.changedFromStops()).isEmpty();
  }

  @Test
  void theClosedFacilitiesAreCarriedToTheNextTransaction() {
    repository.closeFacilities(Set.of(E1));
    publishAndContinue();

    assertThat(repository.freeze().closedFacilityIds()).containsExactly(E1);
  }

  @Test
  void theGivenSetIsCopied() {
    var closed = new HashSet<>(Set.of(E1));
    repository.closeFacilities(closed);
    closed.add(E2);

    assertThat(repository.freeze().closedFacilityIds()).containsExactly(E1);
  }

  @Test
  void aFrozenRepositoryCannotCloseFacilities() {
    lifecycle.freeze(repository);

    assertThrows(IllegalStateException.class, () -> repository.closeFacilities(Set.of(E1)));
  }

  @Test
  void theBuildRepositoryIsNotChanged() {
    repository.closeFacilities(Set.of(E1));

    var next = (DefaultRegularTransferRepository<String>) buildRepository.createRuntimeRepository();
    assertThat(next.freeze().closedFacilityIds()).isEmpty();
  }

  /** Publish the repository in a snapshot, and continue with a repository of a new transaction. */
  private void publishAndContinue() {
    var snapshot = lifecycle.freeze(repository);
    repository = (DefaultRegularTransferRepository<String>) lifecycle.copyOnWrite(snapshot);
  }
}
