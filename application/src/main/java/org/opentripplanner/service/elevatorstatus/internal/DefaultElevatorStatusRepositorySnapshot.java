package org.opentripplanner.service.elevatorstatus.internal;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.opentripplanner.service.elevatorstatus.ElevatorStatusRepositorySnapshot;
import org.opentripplanner.service.elevatorstatus.model.ElevatorStatus;

/** The immutable state of the elevator status repository, published at commit. */
class DefaultElevatorStatusRepositorySnapshot implements ElevatorStatusRepositorySnapshot {

  private final Map<String, ElevatorStatus> statusById;
  private final Set<String> outOfServiceIds;

  DefaultElevatorStatusRepositorySnapshot(Map<String, ElevatorStatus> statusById) {
    this.statusById = Map.copyOf(statusById);
    this.outOfServiceIds = DefaultElevatorStatusRepository.outOfServiceIds(this.statusById);
  }

  @Override
  public Optional<ElevatorStatus> findStatus(String elevatorId) {
    return Optional.ofNullable(statusById.get(elevatorId));
  }

  @Override
  public Collection<ElevatorStatus> listStatuses() {
    return statusById.values();
  }

  @Override
  public Set<String> outOfServiceIds() {
    return outOfServiceIds;
  }

  /** Create a mutable repository initialized with this state. */
  DefaultElevatorStatusRepository copyOnWrite() {
    return new DefaultElevatorStatusRepository(statusById);
  }
}
