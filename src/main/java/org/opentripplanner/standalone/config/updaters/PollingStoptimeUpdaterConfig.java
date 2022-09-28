package org.opentripplanner.standalone.config.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.DataSourceType;
import org.opentripplanner.updater.stoptime.BackwardsDelayPropagationType;
import org.opentripplanner.updater.stoptime.PollingStoptimeUpdaterParameters;
import org.opentripplanner.util.OtpAppException;

public class PollingStoptimeUpdaterConfig {

  public static PollingStoptimeUpdaterParameters create(String configRef, NodeAdapter c) {
    String file = null;
    String url = null;
    String sourceTypeStr = c.asText("sourceType");
    DataSourceType sourceType;

    if ("gtfs-file".equals(sourceTypeStr)) {
      file = c.asText("file");
      sourceType = DataSourceType.GTFS_RT_FILE;
    } else if ("gtfs-http".equals(sourceTypeStr)) {
      url = c.asText("url");
      sourceType = DataSourceType.GTFS_RT_HTTP;
    } else {
      throw new OtpAppException(
        "Polling-stoptime-updater sourece-type is not valid: {}",
        sourceTypeStr
      );
    }

    return new PollingStoptimeUpdaterParameters(
      configRef + ":" + sourceTypeStr,
      c.of("frequencySec").withDoc(NA, /*TODO DOC*/"TODO").asInt(60),
      c.of("logFrequency").withDoc(NA, /*TODO DOC*/"TODO").asInt(-1),
      c.of("maxSnapshotFrequencyMs").withDoc(NA, /*TODO DOC*/"TODO").asInt(-1),
      c.of("purgeExpiredData").withDoc(NA, /*TODO DOC*/"TODO").asBoolean(false),
      c.of("fuzzyTripMatching").withDoc(NA, /*TODO DOC*/"TODO").asBoolean(false),
      c.asEnum("backwardsDelayPropagationType", BackwardsDelayPropagationType.REQUIRED_NO_DATA),
      sourceType,
      c.asText("feedId", null),
      url,
      file
    );
  }
}
