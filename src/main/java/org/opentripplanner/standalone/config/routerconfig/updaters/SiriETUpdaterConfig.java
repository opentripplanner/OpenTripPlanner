package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.util.UUID;
import org.opentripplanner.ext.siri.updater.SiriETUpdaterParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class SiriETUpdaterConfig {

  public static SiriETUpdaterParameters create(String configRef, NodeAdapter c) {
    return new SiriETUpdaterParameters(
      configRef,
      c.of("feedId").withDoc(NA, /*TODO DOC*/"TODO").asString(null),
      c.of("logFrequency").withDoc(NA, /*TODO DOC*/"TODO").asInt(-1),
      c.of("maxSnapshotFrequencyMs").withDoc(NA, /*TODO DOC*/"TODO").asInt(-1),
      c.of("purgeExpiredData").withDoc(NA, /*TODO DOC*/"TODO").asBoolean(false),
      c.of("blockReadinessUntilInitialized").withDoc(NA, /*TODO DOC*/"TODO").asBoolean(false),
      c.of("url").withDoc(NA, /*TODO DOC*/"TODO").asString(),
      c.of("frequencySec").withDoc(NA, /*TODO DOC*/"TODO").asInt(60),
      c.of("requestorRef").withDoc(NA, /*TODO DOC*/"TODO").asString("otp-" + UUID.randomUUID()),
      c.of("timeoutSec").withDoc(NA, /*TODO DOC*/"TODO").asInt(-1),
      c.of("previewIntervalMinutes").withDoc(NA, /*TODO DOC*/"TODO").asInt(-1),
      c.of("fuzzyTripMatching").withDoc(NA, /*TODO DOC*/"TODO").asBoolean(false)
    );
  }
}
