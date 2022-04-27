package org.opentripplanner.ext.vehiclerentalservicedirectory;

import java.util.Map;
import org.opentripplanner.updater.vehicle_rental.datasources.params.GbfsVehicleRentalDataSourceParameters;

public class GbfsDataSourceParameters extends GbfsVehicleRentalDataSourceParameters {

  public GbfsDataSourceParameters(String url, String language, Map<String, String> httpHeaders) {
    super(url, language, false, httpHeaders);
  }
}
