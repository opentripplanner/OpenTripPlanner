package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.routerconfig.updaters.sources.VehicleRentalSourceFactory;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalSourceType;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalUpdaterParameters;

public class VehicleRentalUpdaterConfig {

  public static VehicleRentalUpdaterParameters create(String configRef, NodeAdapter c) {
    var sourceType = c
      .of("sourceType")
      .since(NA)
      .summary("What source of vehicle rental updater to use.")
      .asEnum(VehicleRentalSourceType.class);
    return new VehicleRentalUpdaterParameters(
      configRef + "." + sourceType,
      c
        .of("frequencySec")
        .since(NA)
        .summary("How often the data should be updated in seconds.")
        .asInt(60),
      VehicleRentalSourceFactory.create(sourceType, c)
    );
  }
}
