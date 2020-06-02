package org.opentripplanner.standalone.config.updaters.sources;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.updater.bike_rental.GenericKmlBikeRentalDataSource;

public class GenericKmlBikeRentalSourceParameters extends UpdaterSourceParameters implements
    GenericKmlBikeRentalDataSource.Parameters {

  private final String namePrefix;

  public GenericKmlBikeRentalSourceParameters(NodeAdapter c) {
    super(c);
    namePrefix = c.asText("namePrefix", null);
  }

  public String getNamePrefix() { return namePrefix; }
}
