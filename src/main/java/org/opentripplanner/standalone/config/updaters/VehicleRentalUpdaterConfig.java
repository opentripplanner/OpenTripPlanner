package org.opentripplanner.standalone.config.updaters;

import org.opentripplanner.standalone.config.framework.NodeAdapter;
import org.opentripplanner.standalone.config.updaters.sources.VehicleRentalSourceFactory;
import org.opentripplanner.updater.DataSourceType;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalUpdaterParameters;

public class VehicleRentalUpdaterConfig {

  public static VehicleRentalUpdaterParameters create(String configRef, NodeAdapter c) {
    var sourceType = c.asEnum("sourceType", DataSourceType.class);
    return new VehicleRentalUpdaterParameters(
      configRef + "." + sourceType,
      c.asInt("frequencySec", 60),
      VehicleRentalSourceFactory.create(sourceType, c)
    );
  }
}
