package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.alert.GtfsRealtimeAlertsUpdaterParameters;

public class GtfsRealtimeAlertsUpdaterConfig {

  public static GtfsRealtimeAlertsUpdaterParameters create(String configRef, NodeAdapter c) {
    return new GtfsRealtimeAlertsUpdaterParameters(
      configRef,
      c.of("feedId").withDoc(NA, /*TODO DOC*/"TODO").asString(null),
      c.of("url").withDoc(NA, /*TODO DOC*/"TODO").asString(),
      c.of("earlyStartSec").withDoc(NA, /*TODO DOC*/"TODO").asInt(0),
      c.of("fuzzyTripMatching").withDoc(NA, /*TODO DOC*/"TODO").asBoolean(false),
      c.of("frequencySec").withDoc(NA, /*TODO DOC*/"TODO").asInt(60)
    );
  }
}
