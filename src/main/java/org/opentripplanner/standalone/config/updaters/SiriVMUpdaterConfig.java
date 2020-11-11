package org.opentripplanner.standalone.config.updaters;

import org.opentripplanner.ext.siri.updater.SiriVMUpdaterParameters;
import org.opentripplanner.standalone.config.NodeAdapter;

import java.util.UUID;

public class SiriVMUpdaterConfig {


  public static SiriVMUpdaterParameters create(String configRef, NodeAdapter c) {
    return new SiriVMUpdaterParameters(
        configRef,
        c.asText("feedId", null),
        c.asInt("logFrequency", -1),
        c.asInt("maxSnapshotFrequencyMs", -1),
        c.asBoolean("purgeExpiredData", false),
        c.asBoolean("fuzzyTripMatching", false),
        c.asBoolean("blockReadinessUntilInitialized", false),
        c.asText("url"),
        c.asText("requestorRef", "otp-"+ UUID.randomUUID()),
        c.asInt("frequencySec", 60),
        c.asInt("timeoutSec", -1)
    );
  }
}
