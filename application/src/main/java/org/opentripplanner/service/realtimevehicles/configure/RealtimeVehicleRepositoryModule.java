package org.opentripplanner.service.realtimevehicles.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.core.domain.framework.transaction.RepositoryRegistry;
import org.opentripplanner.core.domain.framework.transaction.api.RepositoryHandle;
import org.opentripplanner.core.framework.transaction.configure.TransitDomain;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepository;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepositorySnapshot;
import org.opentripplanner.service.realtimevehicles.internal.DefaultRealtimeVehicleRepository;
import org.opentripplanner.service.realtimevehicles.internal.RealtimeVehicleRepositoryLifecycle;

@Module
public abstract class RealtimeVehicleRepositoryModule {

  @Provides
  @Singleton
  public static RepositoryHandle<
    RealtimeVehicleRepositorySnapshot,
    RealtimeVehicleRepository
  > realtimeVehicleRepositoryHandle(@TransitDomain RepositoryRegistry repositoryRegistry) {
    return repositoryRegistry.registerRepository(
      new DefaultRealtimeVehicleRepository(),
      new RealtimeVehicleRepositoryLifecycle()
    );
  }
}
