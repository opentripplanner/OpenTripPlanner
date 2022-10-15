package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.trip.BackwardsDelayPropagationType;
import org.opentripplanner.updater.trip.MqttGtfsRealtimeUpdaterParameters;

public class MqttGtfsRealtimeUpdaterConfig {

  public static MqttGtfsRealtimeUpdaterParameters create(String configRef, NodeAdapter c) {
    return new MqttGtfsRealtimeUpdaterParameters(
      configRef,
      c.of("feedId").since(NA).summary("TODO").asString(null),
      c.of("url").since(NA).summary("TODO").asString(),
      c.of("topic").since(NA).summary("TODO").asString(),
      c.of("qos").since(NA).summary("TODO").asInt(0),
      c.of("fuzzyTripMatching").since(NA).summary("TODO").asBoolean(false),
      c
        .of("backwardsDelayPropagationType")
        .since(NA)
        .summary("TODO")
        .asEnum(BackwardsDelayPropagationType.REQUIRED_NO_DATA)
    );
  }
}
