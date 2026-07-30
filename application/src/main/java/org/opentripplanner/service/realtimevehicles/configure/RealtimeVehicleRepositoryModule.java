package org.opentripplanner.service.realtimevehicles.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.framework.transaction.RepositoryRegistry;
import org.opentripplanner.framework.transaction.api.RepositoryHandle;
import org.opentripplanner.service.realtimevehicles.internal.DefaultRealtimeVehicleRepository;
import org.opentripplanner.service.realtimevehicles.internal.DefaultRealtimeVehicleRepositorySnapshot;
import org.opentripplanner.service.realtimevehicles.internal.RealtimeVehicleRepositoryLifecycle;

@Module
public abstract class RealtimeVehicleRepositoryModule {

  @Provides
  @Singleton
  public static RepositoryHandle<
    DefaultRealtimeVehicleRepositorySnapshot,
    DefaultRealtimeVehicleRepository
  > realtimeVehicleRepositoryHandle(RepositoryRegistry repositoryRegistry) {
    return repositoryRegistry.registerRepository(
      new DefaultRealtimeVehicleRepository(),
      new RealtimeVehicleRepositoryLifecycle()
    );
  }
}
