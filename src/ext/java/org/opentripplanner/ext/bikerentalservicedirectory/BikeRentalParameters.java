package org.opentripplanner.ext.bikerentalservicedirectory;

import org.opentripplanner.updater.bike_rental.datasources.params.BikeRentalDataSourceParameters;
import org.opentripplanner.updater.bike_rental.BikeRentalUpdaterParameters;

public class BikeRentalParameters extends BikeRentalUpdaterParameters {

  public BikeRentalParameters(
      String configRef,
      String url,
      int frequencySec,
      BikeRentalDataSourceParameters sourceParameters
  ) {
    super(configRef, url, null, frequencySec, sourceParameters);
  }
}
