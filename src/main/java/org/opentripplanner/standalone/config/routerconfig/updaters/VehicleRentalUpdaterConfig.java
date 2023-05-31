package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V1_5;

import java.time.Duration;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.routerconfig.updaters.sources.VehicleRentalSourceFactory;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalSourceType;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalUpdaterParameters;

public class VehicleRentalUpdaterConfig {

  public static VehicleRentalUpdaterParameters create(String configRef, NodeAdapter c) {
    var sourceType = c
      .of("sourceType")
      .since(V1_5)
      .summary("What source of vehicle rental updater to use.")
      .asEnum(VehicleRentalSourceType.class);
    return new VehicleRentalUpdaterParameters(
      configRef + "." + sourceType,
      c
        .of("frequency")
        .since(V1_5)
        .summary("How often the data should be updated.")
        .asDuration(Duration.ofMinutes(1)),
      VehicleRentalSourceFactory.create(sourceType, c)
    );
  }
}
