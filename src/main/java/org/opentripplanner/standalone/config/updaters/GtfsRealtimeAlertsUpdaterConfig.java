package org.opentripplanner.standalone.config.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.alerts.GtfsRealtimeAlertsUpdaterParameters;

public class GtfsRealtimeAlertsUpdaterConfig {

  public static GtfsRealtimeAlertsUpdaterParameters create(String configRef, NodeAdapter c) {
    return new GtfsRealtimeAlertsUpdaterParameters(
      configRef,
      c.asText("feedId", null),
      c.asText("url"),
      c.asInt("earlyStartSec", 0),
      c.of("fuzzyTripMatching").withDoc(NA, /*TODO DOC*/"TODO").asBoolean(false),
      c.asInt("frequencySec", 60)
    );
  }
}
