package org.opentripplanner.service.vehiclerental.configure;

import dagger.Binds;
import dagger.Module;
import org.opentripplanner.service.vehiclerental.VehicleRentalRepository;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalRepository;

/**
 * Binds the writable {@link VehicleRentalRepository} for the serve phase.
 *
 * <p>The repository exists only there. Zones applied during the graph build travel on the
 * {@code Graph} and are indexed when the repository is created, which happens on first injection -
 * late enough that a combined build-and-serve run sees the zones the build has just written.
 */
@Module
public interface VehicleRentalRepositoryModule {
  @Binds
  VehicleRentalRepository bind(DefaultVehicleRentalRepository repository);
}
