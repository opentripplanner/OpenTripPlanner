package org.opentripplanner.ext.vehiclerentalservicedirectory;

import java.util.Map;
import org.opentripplanner.updater.vehicle_rental.datasources.params.GbfsVehicleRentalDataSourceParameters;

public class GbfsDataSourceParameters extends GbfsVehicleRentalDataSourceParameters {

  public GbfsDataSourceParameters(
    String url,
    String language,
    Map<String, String> httpHeaders,
    String network
  ) {
    super(url, language, false, httpHeaders, network);
  }
}
