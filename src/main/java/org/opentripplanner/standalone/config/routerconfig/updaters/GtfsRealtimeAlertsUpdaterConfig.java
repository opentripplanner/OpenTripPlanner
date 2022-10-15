package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.alert.GtfsRealtimeAlertsUpdaterParameters;

public class GtfsRealtimeAlertsUpdaterConfig {

  public static GtfsRealtimeAlertsUpdaterParameters create(String configRef, NodeAdapter c) {
    return new GtfsRealtimeAlertsUpdaterParameters(
      configRef,
      c.of("feedId").since(NA).summary("TODO").asString(null),
      c.of("url").since(NA).summary("TODO").asString(),
      c.of("earlyStartSec").since(NA).summary("TODO").asInt(0),
      c.of("fuzzyTripMatching").since(NA).summary("TODO").asBoolean(false),
      c.of("frequencySec").since(NA).summary("TODO").asInt(60)
    );
  }
}
