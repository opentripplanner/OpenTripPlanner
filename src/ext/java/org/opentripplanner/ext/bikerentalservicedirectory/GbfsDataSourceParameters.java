package org.opentripplanner.ext.bikerentalservicedirectory;

import org.opentripplanner.updater.DataSourceType;
import org.opentripplanner.updater.bike_rental.GbfsBikeRentalDataSource;

public class GbfsDataSourceParameters implements GbfsBikeRentalDataSource.Parameters {

  private final DataSourceType type;
  private final String url;

  public GbfsDataSourceParameters(DataSourceType type, String url) {
    this.type = type;
    this.url = url;
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
  public DataSourceType type() {
    return type;
  }
}
