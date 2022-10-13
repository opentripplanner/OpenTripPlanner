package org.opentripplanner.standalone.config.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.trip.BackwardsDelayPropagationType;
import org.opentripplanner.updater.trip.MqttGtfsRealtimeUpdaterParameters;

public class MqttGtfsRealtimeUpdaterConfig {

  public static MqttGtfsRealtimeUpdaterParameters create(String configRef, NodeAdapter c) {
    return new MqttGtfsRealtimeUpdaterParameters(
      configRef,
      c.of("feedId").withDoc(NA, /*TODO DOC*/"TODO").withExample(/*TODO DOC*/"TODO").asString(null),
      c.of("url").withDoc(NA, /*TODO DOC*/"TODO").withExample(/*TODO DOC*/"TODO").asString(),
      c.of("topic").withDoc(NA, /*TODO DOC*/"TODO").withExample(/*TODO DOC*/"TODO").asString(),
      c.of("qos").withDoc(NA, /*TODO DOC*/"TODO").asInt(0),
      c.of("fuzzyTripMatching").withDoc(NA, /*TODO DOC*/"TODO").asBoolean(false),
      c
        .of("backwardsDelayPropagationType")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .asEnum(BackwardsDelayPropagationType.REQUIRED_NO_DATA)
    );
  }
}
