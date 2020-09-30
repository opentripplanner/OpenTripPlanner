package org.opentripplanner.standalone.config.updaters;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.standalone.config.updaters.sources.BikeRentalSourceFactory;
import org.opentripplanner.updater.bike_rental.BikeRentalUpdaterParameters;

public class BikeRentalUpdaterConfig {

  public static BikeRentalUpdaterParameters create(String configRef, NodeAdapter c) {
    String sourceType = c.asText("sourceType");
    return new BikeRentalUpdaterParameters(
        configRef + "." + sourceType,
        c.asText("url", null),
        c.asText("networks", null),
        c.asInt("frequencySec", 60),
        BikeRentalSourceFactory.create(sourceType, c)
    );
  }
}
