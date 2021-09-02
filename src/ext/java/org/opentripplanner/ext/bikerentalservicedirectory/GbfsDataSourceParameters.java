package org.opentripplanner.ext.bikerentalservicedirectory;

import org.opentripplanner.updater.bike_rental.datasources.params.GbfsBikeRentalDataSourceParameters;

import java.util.Map;

public class GbfsDataSourceParameters extends GbfsBikeRentalDataSourceParameters {

  public GbfsDataSourceParameters(String url, String network, Map<String, String> httpHeaders) {
    super(url, network, false, false, httpHeaders);
  }
}
