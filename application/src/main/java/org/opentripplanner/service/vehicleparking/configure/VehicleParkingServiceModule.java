package org.opentripplanner.service.vehicleparking.configure;

import dagger.Binds;
import dagger.Module;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingService;
import org.opentripplanner.service.vehicleparking.VehicleParkingService;

@Module
public interface VehicleParkingServiceModule {
  @Binds
  VehicleParkingService bindService(DefaultVehicleParkingService service);
}
