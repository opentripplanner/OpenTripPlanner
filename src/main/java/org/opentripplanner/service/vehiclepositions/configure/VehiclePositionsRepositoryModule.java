package org.opentripplanner.service.vehiclepositions.configure;

import dagger.Binds;
import dagger.Module;
import org.opentripplanner.service.vehiclepositions.VehiclePositionRepository;
import org.opentripplanner.service.vehiclepositions.internal.DefaultVehiclePositionService;

/**
 * The repository is used during application loading phase, so we need to provide
 * a module for the repository as well as the service.
 */
@Module
public interface VehiclePositionsRepositoryModule {
  @Binds
  VehiclePositionRepository bindRepository(DefaultVehiclePositionService repository);
}
