package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.trip.BackwardsDelayPropagationType;
import org.opentripplanner.updater.trip.WebsocketGtfsRealtimeUpdaterParameters;

public class WebsocketGtfsRealtimeUpdaterConfig {

  public static WebsocketGtfsRealtimeUpdaterParameters create(String configRef, NodeAdapter c) {
    return new WebsocketGtfsRealtimeUpdaterParameters(
      configRef,
      c.of("feedId").since(NA).summary("TODO").asString(null),
      c.of("url").since(NA).summary("TODO").asString(null),
      c.of("reconnectPeriodSec").since(NA).summary("TODO").asInt(60),
      c
        .of("backwardsDelayPropagationType")
        .since(NA)
        .summary("TODO")
        .asEnum(BackwardsDelayPropagationType.REQUIRED_NO_DATA)
    );
  }
}
