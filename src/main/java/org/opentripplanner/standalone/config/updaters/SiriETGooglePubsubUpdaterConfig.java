package org.opentripplanner.standalone.config.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import org.opentripplanner.ext.siri.updater.SiriETGooglePubsubUpdaterParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class SiriETGooglePubsubUpdaterConfig {

  public static SiriETGooglePubsubUpdaterParameters create(String configRef, NodeAdapter c) {
    return new SiriETGooglePubsubUpdaterParameters(
      configRef,
      c.asText("feedId", null),
      c.asText("type"),
      c.asText("projectName"),
      c.asText("topicName"),
      c.asText("dataInitializationUrl", null),
      c.of("reconnectPeriodSec").withDoc(NA, /*TODO DOC*/"TODO").asInt(30),
      c.of("purgeExpiredData").withDoc(NA, /*TODO DOC*/"TODO").asBoolean(false)
    );
  }
}
