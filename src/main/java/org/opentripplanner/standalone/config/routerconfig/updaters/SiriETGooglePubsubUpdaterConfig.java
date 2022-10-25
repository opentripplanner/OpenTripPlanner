package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.ext.siri.updater.SiriETGooglePubsubUpdaterParameters.INITIAL_GET_DATA_TIMEOUT;
import static org.opentripplanner.ext.siri.updater.SiriETGooglePubsubUpdaterParameters.RECONNECT_PERIOD;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import org.opentripplanner.ext.siri.updater.SiriETGooglePubsubUpdaterParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class SiriETGooglePubsubUpdaterConfig {

  public static SiriETGooglePubsubUpdaterParameters create(String configRef, NodeAdapter c) {
    return new SiriETGooglePubsubUpdaterParameters(
      configRef,
      c.of("feedId").since(NA).summary("TODO").asString(null),
      c.of("type").since(NA).summary("TODO").asString(),
      c.of("projectName").since(NA).summary("TODO").asString(),
      c.of("topicName").since(NA).summary("TODO").asString(),
      c.of("dataInitializationUrl").since(NA).summary("TODO").asString(null),
      c.of("reconnectPeriod").since(NA).summary("TODO").asDuration(RECONNECT_PERIOD),
      c.of("initialGetDataTimeout").since(NA).summary("TODO").asDuration(INITIAL_GET_DATA_TIMEOUT),
      c.of("purgeExpiredData").since(NA).summary("TODO").asBoolean(false),
      c.of("fuzzyTripMatching").since(NA).summary("TODO").asBoolean(false)
    );
  }
}
