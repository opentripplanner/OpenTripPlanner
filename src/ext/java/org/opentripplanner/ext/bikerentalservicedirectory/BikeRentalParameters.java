package org.opentripplanner.ext.bikerentalservicedirectory;

import org.opentripplanner.updater.bike_rental.datasources.params.BikeRentalDataSourceParameters;
import org.opentripplanner.updater.bike_rental.BikeRentalUpdaterParameters;

public class BikeRentalParameters extends BikeRentalUpdaterParameters {

  public BikeRentalParameters(
      String configRef,
      int frequencySec,
      BikeRentalDataSourceParameters sourceParameters
  ) {
    super(configRef, null, frequencySec, sourceParameters);
  }
}
