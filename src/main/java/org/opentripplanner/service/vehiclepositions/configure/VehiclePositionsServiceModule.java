package org.opentripplanner.service.vehiclepositions.configure;

import dagger.Binds;
import dagger.Module;
import org.opentripplanner.service.vehiclepositions.VehiclePositionService;
import org.opentripplanner.service.vehiclepositions.internal.DefaultVehiclePositionService;

/**
 * The service is used during application serve phase, not loading, so we need to provide
 * a module for the service without the repository, which is injected from the loading phase.
 */
@Module
public interface VehiclePositionsServiceModule {
  @Binds
  VehiclePositionService bindService(DefaultVehiclePositionService service);
}
