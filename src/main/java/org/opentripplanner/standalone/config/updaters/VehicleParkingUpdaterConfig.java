package org.opentripplanner.standalone.config.updaters;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingUpdaterParameters;

public class VehicleParkingUpdaterConfig {
  public static VehicleParkingUpdaterParameters create(String updaterRef, NodeAdapter c) {
    return new VehicleParkingUpdaterParameters(
        updaterRef,
        c.asText("url", null),
        c.asText("feedId", null),
        c.asText("namePrefix", null),
        c.asInt("frequencySec", 60),
        c.asBoolean("zip", false)
    );
  }
}
