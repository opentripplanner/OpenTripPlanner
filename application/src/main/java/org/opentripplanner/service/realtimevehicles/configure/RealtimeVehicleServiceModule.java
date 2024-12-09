package org.opentripplanner.service.realtimevehicles.configure;

import dagger.Binds;
import dagger.Module;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleService;
import org.opentripplanner.service.realtimevehicles.internal.DefaultRealtimeVehicleService;

/**
 * The service is used during application serve phase, not loading, so we need to provide
 * a module for the service without the repository, which is injected from the loading phase.
 */
@Module
public interface RealtimeVehicleServiceModule {
  @Binds
  RealtimeVehicleService bindService(DefaultRealtimeVehicleService service);
}
