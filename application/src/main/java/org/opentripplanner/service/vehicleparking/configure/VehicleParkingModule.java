package org.opentripplanner.service.vehicleparking.configure;

import dagger.Binds;
import dagger.Module;
import org.opentripplanner.service.vehicleparking.VehicleParkingRepository;
import org.opentripplanner.service.vehicleparking.VehicleParkingService;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingService;

@Module
public interface VehicleParkingModule {
  @Binds
  VehicleParkingService bindService(DefaultVehicleParkingService service);

  @Binds
  VehicleParkingRepository bindRepository(DefaultVehicleParkingService service);
}
