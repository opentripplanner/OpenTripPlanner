package org.opentripplanner.updater.trip.gtfs.updater.mqtt;

import org.opentripplanner.updater.trip.UrlUpdaterParameters;
import org.opentripplanner.updater.trip.gtfs.BackwardsDelayPropagationType;
import org.opentripplanner.updater.trip.gtfs.ForwardsDelayPropagationType;

public record MqttGtfsRealtimeUpdaterParameters(
  String configRef,
  String feedId,
  String url,
  String topic,
  int qos,
  boolean fuzzyTripMatching,
  ForwardsDelayPropagationType forwardsDelayPropagationType,
  BackwardsDelayPropagationType backwardsDelayPropagationType
)
  implements UrlUpdaterParameters {}
