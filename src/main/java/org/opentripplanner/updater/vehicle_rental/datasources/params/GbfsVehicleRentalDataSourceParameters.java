package org.opentripplanner.updater.vehicle_rental.datasources.params;

import org.opentripplanner.updater.DataSourceType;

import java.util.Map;

public class GbfsVehicleRentalDataSourceParameters extends VehicleRentalDataSourceParameters {

  private final boolean allowKeepingRentedVehicleAtDestination;
  private final boolean routeAsCar;
  private final String language;

  public GbfsVehicleRentalDataSourceParameters(String url, String language, boolean routeAsCar, boolean allowKeepingRentedVehicleAtDestination, Map<String, String> httpHeaders) {
    super(DataSourceType.GBFS, url, httpHeaders);
    this.language = language;
    this.allowKeepingRentedVehicleAtDestination = allowKeepingRentedVehicleAtDestination;
    this.routeAsCar = routeAsCar;
  }

  public boolean routeAsCar() {
    return routeAsCar;
  }

  public boolean allowKeepingRentedVehicleAtDestination() {
    return allowKeepingRentedVehicleAtDestination;
  }

  public String language() {
    return language;
  }
}
