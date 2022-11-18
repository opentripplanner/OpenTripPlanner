package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.util.UUID;
import org.opentripplanner.ext.siri.updater.SiriETUpdaterParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class SiriETUpdaterConfig {

  public static SiriETUpdaterParameters create(String configRef, NodeAdapter c) {
    return new SiriETUpdaterParameters(
      configRef,
      c.of("feedId").since(NA).summary("TODO").asString(null),
      c.of("blockReadinessUntilInitialized").since(NA).summary("TODO").asBoolean(false),
      c.of("url").since(NA).summary("TODO").asString(),
      c.of("frequencySec").since(NA).summary("TODO").asInt(60),
      c.of("requestorRef").since(NA).summary("TODO").asString("otp-" + UUID.randomUUID()),
      c.of("timeoutSec").since(NA).summary("TODO").asInt(-1),
      c.of("previewIntervalMinutes").since(NA).summary("TODO").asInt(-1),
      c.of("fuzzyTripMatching").since(NA).summary("TODO").asBoolean(false)
    );
  }
}
