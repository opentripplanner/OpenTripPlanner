package org.opentripplanner.ext.vehiclerentalservicedirectory;

import org.opentripplanner.updater.vehicle_rental.datasources.params.VehicleRentalDataSourceParameters;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalUpdaterParameters;

public class VehicleRentalParameters extends VehicleRentalUpdaterParameters {

  public VehicleRentalParameters(
      String configRef,
      int frequencySec,
      VehicleRentalDataSourceParameters sourceParameters
  ) {
    super(configRef, frequencySec, sourceParameters);
  }
}
