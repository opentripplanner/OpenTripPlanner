package org.opentripplanner.service.elevatorstatus.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.service.elevatorstatus.model.ElevatorStatus.inService;
import static org.opentripplanner.service.elevatorstatus.model.ElevatorStatus.outOfService;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DefaultElevatorStatusRepositoryTest {

  private static final String A = "Lutz:1";
  private static final String B = "Koch:2";
  private static final String C = "Schindler:3";

  private final ElevatorStatusRepositoryLifecycle lifecycle =
    new ElevatorStatusRepositoryLifecycle();

  @Test
  void allElevatorsAreInServiceInAnEmptyRepository() {
    var snapshot = lifecycle.freeze(new DefaultElevatorStatusRepository());

    assertEquals(Set.of(), snapshot.outOfServiceIds());
    assertTrue(snapshot.listStatuses().isEmpty());
    assertEquals(Optional.empty(), snapshot.findStatus(A));
  }

  @Test
  void setStatuses() {
    var repository = new DefaultElevatorStatusRepository();
    repository.setStatuses(List.of(outOfService(A), inService(B), outOfService(C)));

    assertEquals(Set.of(A, C), repository.outOfServiceIds());

    var snapshot = lifecycle.freeze(repository);
    assertEquals(Set.of(A, C), snapshot.outOfServiceIds());
    assertEquals(Optional.of(inService(B)), snapshot.findStatus(B));
    assertEquals(3, snapshot.listStatuses().size());
  }

  @Test
  void setStatusesReplacesAllStatuses() {
    var repository = new DefaultElevatorStatusRepository();
    repository.setStatuses(List.of(outOfService(A), outOfService(B)));
    repository.setStatuses(List.of(outOfService(C)));

    var snapshot = lifecycle.freeze(repository);
    assertEquals(Set.of(C), snapshot.outOfServiceIds());
    assertEquals(Optional.empty(), snapshot.findStatus(A));
  }

  @Test
  void anElevatorMayBeListedTwiceWithTheSameStatus() {
    var repository = new DefaultElevatorStatusRepository();
    repository.setStatuses(List.of(outOfService(A), outOfService(A)));

    assertEquals(Set.of(A), repository.outOfServiceIds());
  }

  @Test
  void anElevatorCanNotHaveTwoStatuses() {
    var repository = new DefaultElevatorStatusRepository();

    assertThrows(IllegalArgumentException.class, () ->
      repository.setStatuses(List.of(outOfService(A), inService(A)))
    );
  }

  @Test
  void aNewTransactionStartsFromTheLastSnapshotAndDoesNotChangeIt() {
    var repository = new DefaultElevatorStatusRepository();
    repository.setStatuses(List.of(outOfService(A)));
    var first = lifecycle.freeze(repository);

    var next = lifecycle.copyOnWrite(first);
    assertEquals(Set.of(A), next.outOfServiceIds());

    next.setStatuses(List.of(outOfService(B)));
    var second = lifecycle.freeze(next);

    assertEquals(Set.of(A), first.outOfServiceIds());
    assertEquals(Set.of(B), second.outOfServiceIds());
  }
}
