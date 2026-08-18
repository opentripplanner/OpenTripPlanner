package org.opentripplanner.service.vehiclerental.configure;

import dagger.Binds;
import dagger.Module;
import org.opentripplanner.service.vehiclerental.VehicleRentalRepository;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalRepository;

/** Binds the writable {@link VehicleRentalRepository} for the load phase. */
@Module
public interface VehicleRentalRepositoryModule {
  @Binds
  VehicleRentalRepository bind(DefaultVehicleRentalRepository repository);
}
