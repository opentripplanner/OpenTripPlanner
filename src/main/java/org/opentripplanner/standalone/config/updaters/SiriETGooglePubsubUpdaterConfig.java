package org.opentripplanner.standalone.config.updaters;

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
        c.asInt("reconnectPeriodSec", 30),
        c.asBoolean("purgeExpiredData", false)
    );
  }
}
