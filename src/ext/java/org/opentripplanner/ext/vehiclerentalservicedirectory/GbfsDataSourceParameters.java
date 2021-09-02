package org.opentripplanner.ext.vehiclerentalservicedirectory;

import org.opentripplanner.updater.vehicle_rental.datasources.params.GbfsVehicleRentalDataSourceParameters;

import java.util.Map;

public class GbfsDataSourceParameters extends GbfsVehicleRentalDataSourceParameters {

  public GbfsDataSourceParameters(String url, String network, Map<String, String> httpHeaders) {
    super(url, network, false, false, httpHeaders);
  }
}
