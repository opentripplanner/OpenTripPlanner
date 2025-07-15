package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.EnumMapper.docEnumValueList;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_0;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_8;

import org.opentripplanner.standalone.config.framework.json.EnumMapper;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.trip.gtfs.BackwardsDelayPropagationType;
import org.opentripplanner.updater.trip.gtfs.ForwardsDelayPropagationType;
import org.opentripplanner.updater.trip.gtfs.updater.mqtt.MqttGtfsRealtimeUpdaterParameters;

public class MqttGtfsRealtimeUpdaterConfig {

  public static MqttGtfsRealtimeUpdaterParameters create(String configRef, NodeAdapter c) {
    return new MqttGtfsRealtimeUpdaterParameters(
      configRef,
      c.of("feedId").since(V2_0).summary("The feed id to apply the updates to.").asString(),
      c.of("url").since(V2_0).summary("URL of the MQTT broker.").asString(),
      c.of("topic").since(V2_0).summary("The topic to subscribe to.").asString(),
      c.of("qos").since(V2_0).summary("QOS level.").asInt(0),
      c
        .of("fuzzyTripMatching")
        .since(V2_0)
        .summary("Whether to match trips fuzzily.")
        .asBoolean(false),
      c
        .of("forwardsDelayPropagationType")
        .since(V2_8)
        .summary(ForwardsDelayPropagationType.DEFAULT.typeDescription())
        .description(docEnumValueList(ForwardsDelayPropagationType.values()))
        .asEnum(ForwardsDelayPropagationType.DEFAULT),
      c
        .of("backwardsDelayPropagationType")
        .since(V2_2)
        .summary(BackwardsDelayPropagationType.REQUIRED_NO_DATA.typeDescription())
        .description(docEnumValueList(BackwardsDelayPropagationType.values()))
        .asEnum(BackwardsDelayPropagationType.REQUIRED_NO_DATA)
    );
  }
}
