package org.opentripplanner.standalone.config.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.stoptime.BackwardsDelayPropagationType;
import org.opentripplanner.updater.stoptime.WebsocketGtfsRealtimeUpdaterParameters;

public class WebsocketGtfsRealtimeUpdaterConfig {

  public static WebsocketGtfsRealtimeUpdaterParameters create(String configRef, NodeAdapter c) {
    return new WebsocketGtfsRealtimeUpdaterParameters(
      configRef,
      c.asText("feedId", null),
      c.asText("url", null),
      c.of("reconnectPeriodSec").withDoc(NA, /*TODO DOC*/"TODO").asInt(60),
      c.asEnum("backwardsDelayPropagationType", BackwardsDelayPropagationType.REQUIRED_NO_DATA)
    );
  }
}
