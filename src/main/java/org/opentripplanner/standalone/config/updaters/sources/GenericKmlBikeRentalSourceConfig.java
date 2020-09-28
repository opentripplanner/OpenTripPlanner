package org.opentripplanner.standalone.config.updaters.sources;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.updater.bike_rental.GenericKmlBikeRentalDataSource;

public class GenericKmlBikeRentalSourceConfig extends UpdaterSourceConfig implements
    GenericKmlBikeRentalDataSource.Parameters {

  private final String namePrefix;

  public GenericKmlBikeRentalSourceConfig(NodeAdapter c) {
    super(c);
    namePrefix = c.asText("namePrefix", null);
  }

  public String getNamePrefix() { return namePrefix; }
}
