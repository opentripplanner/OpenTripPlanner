package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_0;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;

import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.trip.gtfs.BackwardsDelayPropagationType;
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
        .of("backwardsDelayPropagationType")
        .since(V2_2)
        .summary("How backwards propagation should be handled.")
        .description(
          """
            REQUIRED_NO_DATA:
            Default value. Only propagates delays backwards when it is required to ensure that the times
            are increasing, and it sets the NO_DATA flag on the stops so these automatically updated times
            are not exposed through APIs.

            REQUIRED:
            Only propagates delays backwards when it is required to ensure that the times are increasing.
            The updated times are exposed through APIs.

            ALWAYS:
            Propagates delays backwards on stops with no estimates regardless if it's required or not.
            The updated times are exposed through APIs.
          """
        )
        .asEnum(BackwardsDelayPropagationType.REQUIRED_NO_DATA)
    );
  }
}
