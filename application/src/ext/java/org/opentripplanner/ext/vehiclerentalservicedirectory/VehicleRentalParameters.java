package org.opentripplanner.ext.vehiclerentalservicedirectory;

import java.time.Duration;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalUpdaterParameters;
import org.opentripplanner.updater.vehicle_rental.datasources.params.VehicleRentalDataSourceParameters;

public class VehicleRentalParameters extends VehicleRentalUpdaterParameters {

  public VehicleRentalParameters(
    String configRef,
    Duration frequency,
    VehicleRentalDataSourceParameters sourceParameters
  ) {
    super(configRef, frequency, sourceParameters);
  }
}
