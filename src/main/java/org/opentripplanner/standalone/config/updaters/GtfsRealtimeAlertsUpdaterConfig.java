package org.opentripplanner.standalone.config.updaters;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.updater.alerts.GtfsRealtimeAlertsUpdaterParameters;

public class GtfsRealtimeAlertsUpdaterConfig {

  public static GtfsRealtimeAlertsUpdaterParameters create(String configRef, NodeAdapter c) {
    return new GtfsRealtimeAlertsUpdaterParameters(
        configRef,
        c.asText("feedId", null),
        c.asText("url"),
        c.asInt("earlyStartSec", 0),
        c.asBoolean("fuzzyTripMatching", false),
        c.asInt("frequencySec", 60)
    );
  }
}
