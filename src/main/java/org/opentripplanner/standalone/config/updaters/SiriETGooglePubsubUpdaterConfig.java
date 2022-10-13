package org.opentripplanner.standalone.config.updaters;

import static org.opentripplanner.ext.siri.updater.SiriETGooglePubsubUpdaterParameters.INITIAL_GET_DATA_TIMEOUT;
import static org.opentripplanner.ext.siri.updater.SiriETGooglePubsubUpdaterParameters.RECONNECT_PERIOD;

import org.opentripplanner.ext.siri.updater.SiriETGooglePubsubUpdaterParameters;
import org.opentripplanner.standalone.config.NodeAdapter;

public class SiriETGooglePubsubUpdaterConfig {

  public static SiriETGooglePubsubUpdaterParameters create(String configRef, NodeAdapter c) {
    return new SiriETGooglePubsubUpdaterParameters(
      configRef,
      c.asText("feedId", null),
      c.asText("type"),
      c.asText("projectName"),
      c.asText("topicName"),
      c.asText("dataInitializationUrl", null),
      c.asDuration("reconnectPeriod", RECONNECT_PERIOD),
      c.asDuration("initialGetDataTimeout", INITIAL_GET_DATA_TIMEOUT),
      c.asBoolean("purgeExpiredData", false),
      c.asBoolean("fuzzyTripMatching", false)
    );
  }
}
