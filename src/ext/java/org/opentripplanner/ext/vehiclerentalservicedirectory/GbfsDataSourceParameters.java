package org.opentripplanner.ext.vehiclerentalservicedirectory;

import org.opentripplanner.updater.vehicle_rental.datasources.params.GbfsVehicleRentalDataSourceParameters;

import java.util.Map;

public class GbfsDataSourceParameters extends GbfsVehicleRentalDataSourceParameters {

  public GbfsDataSourceParameters(String url, String language, Map<String, String> httpHeaders) {
    super(url, language, false, httpHeaders);
  }
}
