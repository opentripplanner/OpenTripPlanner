package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V1_5;

import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.trip.BackwardsDelayPropagationType;
import org.opentripplanner.updater.trip.WebsocketGtfsRealtimeUpdaterParameters;

public class WebsocketGtfsRealtimeUpdaterConfig {

  public static WebsocketGtfsRealtimeUpdaterParameters create(String configRef, NodeAdapter c) {
    return new WebsocketGtfsRealtimeUpdaterParameters(
      configRef,
      c.of("feedId").since(V1_5).summary("TODO").asString(),
      c.of("url").since(V1_5).summary("TODO").asString(null),
      c.of("reconnectPeriodSec").since(V1_5).summary("TODO").asInt(60),
      c
        .of("backwardsDelayPropagationType")
        .since(V1_5)
        .summary("TODO")
        .asEnum(BackwardsDelayPropagationType.REQUIRED_NO_DATA)
    );
  }
}
