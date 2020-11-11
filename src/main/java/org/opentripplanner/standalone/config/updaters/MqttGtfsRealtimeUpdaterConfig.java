package org.opentripplanner.standalone.config.updaters;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.updater.stoptime.MqttGtfsRealtimeUpdaterParameters;

public class MqttGtfsRealtimeUpdaterConfig {
  public static MqttGtfsRealtimeUpdaterParameters create(String configRef, NodeAdapter c) {
    return new MqttGtfsRealtimeUpdaterParameters(
        configRef,
        c.asText("feedId", null),
        c.asText("url"),
        c.asText("topic"),
        c.asInt("qos", 0),
        c.asBoolean("fuzzyTripMatching", false)
    );
  }
}
