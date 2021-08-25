package org.opentripplanner.updater.vehicle_rental.datasources.params;

import org.opentripplanner.updater.DataSourceType;

import java.util.Map;

public class GbfsVehicleRentalDataSourceParameters extends VehicleRentalDataSourceParameters {

  private final boolean allowKeepingRentedBicycleAtDestination;
  private final boolean routeAsCar;

  public GbfsVehicleRentalDataSourceParameters(String url, String network, boolean routeAsCar, boolean allowKeepingRentedBicycleAtDestination, Map<String, String> httpHeaders) {
    super(DataSourceType.GBFS, url, network, httpHeaders);
    this.allowKeepingRentedBicycleAtDestination = allowKeepingRentedBicycleAtDestination;
    this.routeAsCar = routeAsCar;
  }

  public boolean routeAsCar() {
    return routeAsCar;
  }

  public boolean allowKeepingRentedBicycleAtDestination() {
    return allowKeepingRentedBicycleAtDestination;
  }
}
