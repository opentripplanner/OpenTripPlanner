package org.opentripplanner.standalone.config.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.stoptime.BackwardsDelayPropagationType;
import org.opentripplanner.updater.stoptime.MqttGtfsRealtimeUpdaterParameters;

public class MqttGtfsRealtimeUpdaterConfig {

  public static MqttGtfsRealtimeUpdaterParameters create(String configRef, NodeAdapter c) {
    return new MqttGtfsRealtimeUpdaterParameters(
      configRef,
      c.asText("feedId", null),
      c.asText("url"),
      c.asText("topic"),
      c.asInt("qos", 0),
      c.of("fuzzyTripMatching").withDoc(NA, /*TODO DOC*/"TODO").asBoolean(false),
      c.asEnum("backwardsDelayPropagationType", BackwardsDelayPropagationType.REQUIRED_NO_DATA)
    );
  }
}
