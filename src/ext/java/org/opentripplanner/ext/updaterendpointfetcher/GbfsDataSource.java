package org.opentripplanner.ext.updaterendpointfetcher;

import org.opentripplanner.updater.bike_rental.GbfsBikeRentalDataSource;

public class GbfsDataSource implements GbfsBikeRentalDataSource.Parameters {

  private final String url;

  private final String name;

  public GbfsDataSource(String url, String name) {
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
