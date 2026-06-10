package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V1_5;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_10;

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
      c
        .of("startupRetryPeriod")
        .since(V2_10)
        .summary(
          "How long to retry loading the vehicle rental data source on startup if it initially fails."
        )
        .description(
          """
          The first time the data source is loaded, OTP will retry for this duration every
          5 seconds before giving up. This is useful to handle temporary network failures during
          OTP startup. Set to `PT0S` to disable retries.
          """
        )
        .asDuration(Duration.ZERO),
      VehicleRentalSourceFactory.create(sourceType, c)
    );
  }
}
