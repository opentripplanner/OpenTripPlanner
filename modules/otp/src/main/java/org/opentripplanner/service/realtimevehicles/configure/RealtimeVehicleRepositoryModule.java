package org.opentripplanner.service.realtimevehicles.configure;

import dagger.Binds;
import dagger.Module;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepository;
import org.opentripplanner.service.realtimevehicles.internal.DefaultRealtimeVehicleService;

/**
 * The repository is used during application loading phase, so we need to provide
 * a module for the repository as well as the service.
 */
@Module
public interface RealtimeVehicleRepositoryModule {
  @Binds
  RealtimeVehicleRepository bindRepository(DefaultRealtimeVehicleService repository);
}
