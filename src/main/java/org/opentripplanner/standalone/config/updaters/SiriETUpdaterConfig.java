package org.opentripplanner.standalone.config.updaters;

import org.opentripplanner.ext.siri.updater.SiriETUpdaterParameters;
import org.opentripplanner.standalone.config.NodeAdapter;

import java.util.UUID;

public class SiriETUpdaterConfig {

  public static SiriETUpdaterParameters create(String configRef, NodeAdapter c) {
    return new SiriETUpdaterParameters(
        configRef,
        c.asText("feedId", null),
        c.asInt("logFrequency", -1),
        c.asInt("maxSnapshotFrequencyMs", -1),
        c.asBoolean("purgeExpiredData", false),
        c.asBoolean("blockReadinessUntilInitialized", false),
        c.asText("url"),
        c.asInt("frequencySec", 60),
        c.asText("requestorRef", "otp-"+ UUID.randomUUID()),
        c.asInt("timeoutSec", -1),
        c.asInt("previewIntervalMinutes", -1)
    );
  }
}
