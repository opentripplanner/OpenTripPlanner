package org.opentripplanner.service.vehicleparking.configure;

import dagger.Binds;
import dagger.Module;
import org.opentripplanner.service.vehicleparking.VehicleParkingService;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingService;

@Module
public interface VehicleParkingServiceModule {
  @Binds
  VehicleParkingService bind(DefaultVehicleParkingService service);
}
