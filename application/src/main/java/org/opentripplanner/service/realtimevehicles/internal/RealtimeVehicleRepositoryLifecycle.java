package org.opentripplanner.service.realtimevehicles.internal;

import org.opentripplanner.framework.transaction.api.RepositoryLifecycle;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepository;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepositorySnapshot;

/**
 * Copy-on-write / freeze lifecycle for the realtime-vehicle repository. Each transaction that
 * writes vehicles gets a new mutable repository initialized from the last committed snapshot, and
 * a new immutable snapshot is published when the transaction commits. Edits made to a repository
 * that is never frozen are simply discarded — this supports transaction rollback in the future.
 */
public class RealtimeVehicleRepositoryLifecycle
  implements RepositoryLifecycle<RealtimeVehicleRepositorySnapshot, RealtimeVehicleRepository> {

  @Override
  public RealtimeVehicleRepository copyOnWrite(RealtimeVehicleRepositorySnapshot snapshot) {
    return new DefaultRealtimeVehicleRepository(snapshot);
  }

  @Override
  public RealtimeVehicleRepositorySnapshot freeze(RealtimeVehicleRepository repository) {
    return repository.createSnapshot();
  }
}
