package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.util.UUID;
import org.opentripplanner.ext.siri.updater.SiriVMUpdaterParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class SiriVMUpdaterConfig {

  public static SiriVMUpdaterParameters create(String configRef, NodeAdapter c) {
    return new SiriVMUpdaterParameters(
      configRef,
      c.of("feedId").withDoc(NA, /*TODO DOC*/"TODO").asString(null),
      c.of("logFrequency").withDoc(NA, /*TODO DOC*/"TODO").asInt(-1),
      c.of("maxSnapshotFrequencyMs").withDoc(NA, /*TODO DOC*/"TODO").asInt(-1),
      c.of("purgeExpiredData").withDoc(NA, /*TODO DOC*/"TODO").asBoolean(false),
      c.of("blockReadinessUntilInitialized").withDoc(NA, /*TODO DOC*/"TODO").asBoolean(false),
      c.of("url").withDoc(NA, /*TODO DOC*/"TODO").asString(),
      c.of("requestorRef").withDoc(NA, /*TODO DOC*/"TODO").asString("otp-" + UUID.randomUUID()),
      c.of("frequencySec").withDoc(NA, /*TODO DOC*/"TODO").asInt(60),
      c.of("timeoutSec").withDoc(NA, /*TODO DOC*/"TODO").asInt(-1)
    );
  }
}
