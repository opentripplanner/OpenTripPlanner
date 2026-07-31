package org.opentripplanner.service.realtimevehicles.configure;

import dagger.Binds;
import dagger.Module;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepository;
import org.opentripplanner.service.realtimevehicles.internal.DefaultRealtimeVehicleRepository;

@Module
public interface RealtimeVehicleRepositoryModule {
  @Binds
  RealtimeVehicleRepository bindRepository(DefaultRealtimeVehicleRepository repository);
}
