package org.opentripplanner.ext.bikerentalservicedirectory;

import org.opentripplanner.updater.bike_rental.datasources.params.GbfsBikeRentalDataSourceParameters;

public class GbfsDataSourceParameters extends GbfsBikeRentalDataSourceParameters {

  public GbfsDataSourceParameters(String url, String network) {
    super(url, network, false);
  }
}
