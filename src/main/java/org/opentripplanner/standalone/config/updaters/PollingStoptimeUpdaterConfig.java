package org.opentripplanner.standalone.config.updaters;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.updater.DataSourceType;
import org.opentripplanner.updater.stoptime.BackwardsDelayPropagationType;
import org.opentripplanner.updater.stoptime.PollingTripUpdaterParameters;
import org.opentripplanner.util.OtpAppException;

public class PollingStoptimeUpdaterConfig {

  public static PollingTripUpdaterParameters create(String configRef, NodeAdapter c) {
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

    return new PollingTripUpdaterParameters(
      configRef + ":" + sourceTypeStr,
      c.asInt("frequencySec", 60),
      c.asInt("maxSnapshotFrequencyMs", -1),
      c.asBoolean("purgeExpiredData", false),
      c.asBoolean("fuzzyTripMatching", false),
      c.asEnum("backwardsDelayPropagationType", BackwardsDelayPropagationType.REQUIRED_NO_DATA),
      sourceType,
      c.asText("feedId", null),
      url,
      file
    );
  }
}
