package org.opentripplanner.service.elevatorstatus.internal;

import org.opentripplanner.core.model.transaction.RepositoryLifecycle;
import org.opentripplanner.service.elevatorstatus.ElevatorStatusRepository;
import org.opentripplanner.service.elevatorstatus.ElevatorStatusRepositorySnapshot;

/**
 * Copy-on-write and freeze of the elevator status repository. Each transaction that writes the
 * status gets a new mutable repository initialized from the last committed snapshot, and a new
 * immutable snapshot is published when the transaction commits.
 */
public class ElevatorStatusRepositoryLifecycle
  implements RepositoryLifecycle<ElevatorStatusRepositorySnapshot, ElevatorStatusRepository>
{

  @Override
  public ElevatorStatusRepository copyOnWrite(ElevatorStatusRepositorySnapshot snapshot) {
    // the cast is safe: all snapshots are created by freeze() below
    return ((DefaultElevatorStatusRepositorySnapshot) snapshot).copyOnWrite();
  }

  @Override
  public ElevatorStatusRepositorySnapshot freeze(ElevatorStatusRepository repository) {
    // the cast is safe: all repositories are created by copyOnWrite() above or by the module
    return ((DefaultElevatorStatusRepository) repository).freeze();
  }
}
