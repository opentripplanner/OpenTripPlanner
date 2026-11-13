package org.opentripplanner.service.elevatorstatus;

import java.util.Collection;
import java.util.Set;
import org.opentripplanner.service.elevatorstatus.model.ElevatorStatus;

/**
 * The mutable repository of the status of the elevators. It is the source of truth for which
 * elevators are in service and which are out of service. An elevator without a status is in
 * service.
 * <p>
 * It is managed by the transaction framework of the transit domain: a new repository, initialized
 * from the last committed {@link ElevatorStatusRepositorySnapshot}, is created for each transaction
 * that writes to it, and is only used on the transit writer thread. A new snapshot is published when
 * the transaction commits.
 */
public interface ElevatorStatusRepository {
  /**
   * Replace the status of all elevators with the given statuses. The elevators not in
   * {@code statuses} have no status afterward, so they are in service.
   *
   * @throws IllegalArgumentException if an elevator id is in {@code statuses} more than once with
   *                                  different statuses
   */
  void setStatuses(Collection<ElevatorStatus> statuses);

  /** The ids of the elevators that are out of service. */
  Set<String> outOfServiceIds();
}
