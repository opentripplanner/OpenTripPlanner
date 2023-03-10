package org.opentripplanner.service.vehiclerental.configure;

import dagger.Binds;
import dagger.Module;
import org.opentripplanner.service.vehiclerental.VehicleRentalRepository;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalService;

/**
 * The service is used during application serve phase, not loading, so we need to provide
 * a module for the service without the repository, which is injected from the loading phase.
 */
@Module
public interface VehicleRentalRepositoryModule {
  @Binds
  VehicleRentalRepository bindService(DefaultVehicleRentalService service);
}
