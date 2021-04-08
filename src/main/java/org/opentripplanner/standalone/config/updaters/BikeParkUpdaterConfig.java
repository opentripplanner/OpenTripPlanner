package org.opentripplanner.standalone.config.updaters;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.updater.bike_park.BikeParkUpdaterParameters;

public class BikeParkUpdaterConfig {
  public static BikeParkUpdaterParameters create(String updaterRef, NodeAdapter c) {
    return new BikeParkUpdaterParameters(
        updaterRef,
        c.asText("url", null),
        c.asText("namePrefix", null),
        c.asInt("frequencySec", 60),
        c.asBoolean("zip", false)
    );
  }
}
