package org.opentripplanner.service.elevatorstatus.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.service.elevatorstatus.ElevatorStatusRepository;
import org.opentripplanner.service.elevatorstatus.ElevatorStatusRepositorySnapshot;
import org.opentripplanner.service.elevatorstatus.model.ElevatorStatus;

/**
 * The mutable repository of the elevator status. A new instance is created for each transaction
 * that writes to it, initialized from the last committed snapshot, and only used on the writer
 * thread.
 */
public class DefaultElevatorStatusRepository implements ElevatorStatusRepository {

  private Map<String, ElevatorStatus> statusById;

  /** Create an empty repository: all elevators are in service. */
  public DefaultElevatorStatusRepository() {
    this(Map.of());
  }

  DefaultElevatorStatusRepository(Map<String, ElevatorStatus> statusById) {
    this.statusById = statusById;
  }

  @Override
  public void setStatuses(Collection<ElevatorStatus> statuses) {
    var result = new HashMap<String, ElevatorStatus>();
    for (var status : statuses) {
      var previous = result.put(status.elevatorId(), status);
      if (previous != null && !previous.equals(status)) {
        throw new IllegalArgumentException(
          "Elevator " + status.elevatorId() + " has more than one status: " + statuses
        );
      }
    }
    statusById = Map.copyOf(result);
  }

  @Override
  public Set<String> outOfServiceIds() {
    return outOfServiceIds(statusById);
  }

  /** Produce an immutable snapshot of the state. */
  ElevatorStatusRepositorySnapshot freeze() {
    return new DefaultElevatorStatusRepositorySnapshot(statusById);
  }

  static Set<String> outOfServiceIds(Map<String, ElevatorStatus> statusById) {
    return statusById
      .values()
      .stream()
      .filter(status -> !status.inService())
      .map(ElevatorStatus::elevatorId)
      .collect(Collectors.toUnmodifiableSet());
  }
}
