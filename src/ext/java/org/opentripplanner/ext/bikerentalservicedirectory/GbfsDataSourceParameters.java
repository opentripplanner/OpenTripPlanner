package org.opentripplanner.ext.bikerentalservicedirectory;

import org.opentripplanner.updater.bike_rental.GbfsBikeRentalDataSource;

public class GbfsDataSourceParameters implements GbfsBikeRentalDataSource.Parameters {

  private final String url;

  private final String name;

  public GbfsDataSourceParameters(String url, String name) {
    this.url = url;
    this.name = name;
  }

  @Override
  public boolean routeAsCar() {
    return false;
  }

  @Override
  public String getUrl() {
    return url;
  }

  @Override
  public String getName() {
    return name;
  }
}
