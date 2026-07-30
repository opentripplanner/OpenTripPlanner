package org.opentripplanner.service.realtimevehicles.internal;

import org.opentripplanner.framework.transaction.api.RepositoryLifecycle;

/**
 * Copy-on-write / freeze lifecycle for the realtime-vehicle repository. Each transaction that
 * writes vehicles gets a new mutable repository initialized from the last committed snapshot, and
 * a new immutable snapshot is published when the transaction commits.
 */
public class RealtimeVehicleRepositoryLifecycle
  implements
    RepositoryLifecycle<
      DefaultRealtimeVehicleRepositorySnapshot,
      DefaultRealtimeVehicleRepository
    > {

  @Override
  public DefaultRealtimeVehicleRepository copyOnWrite(
    DefaultRealtimeVehicleRepositorySnapshot snapshot
  ) {
    return snapshot.copyOnWrite();
  }

  @Override
  public DefaultRealtimeVehicleRepositorySnapshot freeze(
    DefaultRealtimeVehicleRepository repository
  ) {
    return repository.freeze();
  }
}
