package org.opentripplanner.service.vehicleparking.configure;

import dagger.Binds;
import dagger.Module;
import org.opentripplanner.service.vehicleparking.VehicleParkingRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;

@Module
public interface VehicleParkingRepositoryModule {
  @Binds
  VehicleParkingRepository bind(DefaultVehicleParkingRepository repo);
}
