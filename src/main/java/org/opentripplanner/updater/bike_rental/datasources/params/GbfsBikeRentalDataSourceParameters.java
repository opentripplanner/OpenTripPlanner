package org.opentripplanner.updater.bike_rental.datasources.params;

import org.opentripplanner.updater.DataSourceType;

public class GbfsBikeRentalDataSourceParameters extends BikeRentalDataSourceParameters {

  private final boolean routeAsCar;

  public GbfsBikeRentalDataSourceParameters(String url, String network, boolean routeAsCar) {
    super(DataSourceType.GBFS, url, network, null);
    this.routeAsCar = routeAsCar;
  }

  public boolean routeAsCar() {
    return routeAsCar;
  }

  @Override
  public String getApiKey() { throw new UnsupportedOperationException(); }

}
