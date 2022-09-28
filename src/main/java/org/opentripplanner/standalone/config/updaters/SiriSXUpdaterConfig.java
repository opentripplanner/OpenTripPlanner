package org.opentripplanner.standalone.config.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.util.UUID;
import org.opentripplanner.ext.siri.updater.SiriSXUpdaterParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class SiriSXUpdaterConfig {

  public static SiriSXUpdaterParameters create(String configRef, NodeAdapter c) {
    return new SiriSXUpdaterParameters(
      configRef,
      c.asText("feedId", null),
      c.asText("url"),
      c.asText("requestorRef", "otp-" + UUID.randomUUID()),
      c.of("frequencySec").withDoc(NA, /*TODO DOC*/"TODO").asInt(60),
      c.of("earlyStartSec").withDoc(NA, /*TODO DOC*/"TODO").asInt(-1),
      c.of("timeoutSec").withDoc(NA, /*TODO DOC*/"TODO").asInt(-1),
      c.of("blockReadinessUntilInitialized").withDoc(NA, /*TODO DOC*/"TODO").asBoolean(false)
    );
  }
}
