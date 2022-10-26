package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.util.UUID;
import org.opentripplanner.ext.siri.updater.SiriSXUpdaterParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class SiriSXUpdaterConfig {

  public static SiriSXUpdaterParameters create(String configRef, NodeAdapter c) {
    return new SiriSXUpdaterParameters(
      configRef,
      c.of("feedId").since(NA).summary("TODO").asString(null),
      c.of("url").since(NA).summary("TODO").asString(),
      c.of("requestorRef").since(NA).summary("TODO").asString("otp-" + UUID.randomUUID()),
      c.of("frequencySec").since(NA).summary("TODO").asInt(60),
      c.of("earlyStartSec").since(NA).summary("TODO").asInt(-1),
      c.of("timeoutSec").since(NA).summary("TODO").asInt(-1),
      c.of("blockReadinessUntilInitialized").since(NA).summary("TODO").asBoolean(false)
    );
  }
}
