package org.opentripplanner.updater.bike_rental.datasources.params;

import org.opentripplanner.updater.DataSourceType;

import java.util.Map;

public class GenericKmlBikeRentalDataSourceParameters extends BikeRentalDataSourceParameters {

  private final String namePrefix;

  public GenericKmlBikeRentalDataSourceParameters(String url, String namePrefix) {
    super(DataSourceType.KML, url, null, null, Map.of());
    this.namePrefix = namePrefix;
  }

  public String getNamePrefix() {
    return namePrefix;
  }
}
