package org.opentripplanner.standalone.config.updaters;

import org.opentripplanner.ext.siri.updater.SiriSXUpdaterParameters;
import org.opentripplanner.standalone.config.NodeAdapter;

import java.util.UUID;

public class SiriSXUpdaterConfig {
  public static SiriSXUpdaterParameters create(String configRef, NodeAdapter c) {
    return new SiriSXUpdaterParameters(
        configRef,
        c.asText("feedId", null),
        c.asText("url"),
        c.asText("requestorRef", "otp-"+ UUID.randomUUID()),
        c.asInt("frequencySec", 60),
        c.asInt("earlyStartSec", -1),
        c.asInt("timeoutSec", -1),
        c.asBoolean("blockReadinessUntilInitialized", false)
    );
  }
}
