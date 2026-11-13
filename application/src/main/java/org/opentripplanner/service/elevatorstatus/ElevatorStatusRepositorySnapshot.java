package org.opentripplanner.service.elevatorstatus;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.opentripplanner.service.elevatorstatus.model.ElevatorStatus;

/**
 * An immutable, read-only snapshot of the {@link ElevatorStatusRepository}. A new snapshot is
 * published each time a transaction that touched the repository commits.
 */
public interface ElevatorStatusRepositorySnapshot {
  /** The status of the elevator, empty if it has none: then it is in service. */
  Optional<ElevatorStatus> findStatus(String elevatorId);

  /** All the statuses. */
  Collection<ElevatorStatus> listStatuses();

  /** The ids of the elevators that are out of service. */
  Set<String> outOfServiceIds();
}
