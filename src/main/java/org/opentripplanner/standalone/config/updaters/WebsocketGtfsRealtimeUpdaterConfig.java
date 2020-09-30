package org.opentripplanner.standalone.config.updaters;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.updater.stoptime.WebsocketGtfsRealtimeUpdaterParameters;

public class WebsocketGtfsRealtimeUpdaterConfig {
  public static WebsocketGtfsRealtimeUpdaterParameters create(String configRef, NodeAdapter c) {
    return new WebsocketGtfsRealtimeUpdaterParameters(
        configRef,
        c.asText("feedId", null),
        c.asText("url", null),
        c.asInt("reconnectPeriodSec", 60)
    );
  }
}