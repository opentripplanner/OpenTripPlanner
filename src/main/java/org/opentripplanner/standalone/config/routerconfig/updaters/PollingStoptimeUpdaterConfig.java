package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.DataSourceType;
import org.opentripplanner.updater.trip.BackwardsDelayPropagationType;
import org.opentripplanner.updater.trip.PollingTripUpdaterParameters;
import org.opentripplanner.util.OtpAppException;

public class PollingStoptimeUpdaterConfig {

  public static PollingTripUpdaterParameters create(String configRef, NodeAdapter c) {
    String file = null;
    String url = null;

    if (c.exist("file")) {
      file = c.of("file").since(NA).summary("TODO").asString();
    } else if (c.exist("url")) {
      url = c.of("url").since(NA).summary("TODO").asString();
    } else {
      throw new OtpAppException(
        "Need either 'url' or 'file' properties to configure " +
        configRef +
        " but received: " +
        c.asText()
      );
    }

    // TODO DOC: Deprecate  c.of("logFrequency").withDoc(NA, /*TODO DOC*/"TODO").asInt(-1)

    return new PollingTripUpdaterParameters(
      configRef,
      c.of("frequencySec").since(NA).summary("TODO").asInt(60),
      c.of("maxSnapshotFrequencyMs").since(NA).summary("TODO").asInt(-1),
      c.of("purgeExpiredData").since(NA).summary("TODO").asBoolean(false),
      c.of("fuzzyTripMatching").since(NA).summary("TODO").asBoolean(false),
      c
        .of("backwardsDelayPropagationType")
        .since(NA)
        .summary("TODO")
        .asEnum(BackwardsDelayPropagationType.REQUIRED_NO_DATA),
      c.of("feedId").since(NA).summary("TODO").asString(null),
      url,
      file
    );
  }
}
