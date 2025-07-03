package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_0;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_8;

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
        .summary("How forwards propagation should be handled.")
        .description(
          """
            NONE:
            Do not propagate delays forwards. Reject real-time updates if not all arrival / departure times
            are specified until the end of the trip.

            Note that this will also reject all updates containing NO_DATA, or all updates containing
            SKIPPED stops without a time provided. Only use this value when you can guarantee that the
            real-time feed contains all departure and arrival times for all future stops, including
            SKIPPED stops.

            DEFAULT:
            Default value. Propagate delays forwards for stops without arrival / departure times given.

            For NO_DATA stops, the scheduled time is used unless a previous delay fouls the scheduled time
            at the stop, in such case the minimum amount of delay is propagated to make the times
            non-decreasing.

            For SKIPPED stops without time given, interpolate the estimated time using the ratio between
            scheduled and real times from the previous to the next stop.
          """
        )
        .asEnum(ForwardsDelayPropagationType.DEFAULT),
      c
        .of("backwardsDelayPropagationType")
        .since(V2_2)
        .summary("How backwards propagation should be handled.")
        .description(
          """
            NONE:
            Do not propagate delays backwards. Reject real-time updates if the times are not specified
            from the beginning of the trip.

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
