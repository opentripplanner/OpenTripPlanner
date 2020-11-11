package org.opentripplanner.updater.bike_rental.datasources.params;

import org.opentripplanner.updater.DataSourceType;

public class GenericKmlBikeRentalDataSourceParameters extends BikeRentalDataSourceParameters {

  private final String namePrefix;

  public GenericKmlBikeRentalDataSourceParameters(String url, String namePrefix) {
    super(DataSourceType.KML, url, null, null);
    this.namePrefix = namePrefix;
  }

  public String getNamePrefix() {
    return namePrefix;
  }
}
