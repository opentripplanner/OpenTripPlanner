package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V1_5;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_3;

import java.time.Duration;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.alert.gtfs.GtfsRealtimeAlertsUpdaterParameters;

public class GtfsRealtimeAlertsUpdaterConfig {

  public static GtfsRealtimeAlertsUpdaterParameters create(String configRef, NodeAdapter c) {
    return new GtfsRealtimeAlertsUpdaterParameters(
      configRef,
      c.of("feedId").since(V1_5).summary("The id of the feed to apply the alerts to.").asString(),
      c.of("url").since(V1_5).summary("URL to fetch the GTFS-RT feed from.").asString(),
      c
        .of("earlyStartSec")
        .since(V1_5)
        .summary("How long before the posted start of an event it should be displayed to users")
        .asInt(0),
      c
        .of("fuzzyTripMatching")
        .since(V1_5)
        .summary("Whether to match trips fuzzily.")
        .asBoolean(false),
      c
        .of("frequency")
        .since(V1_5)
        .summary("How often the URL should be fetched.")
        .asDuration(Duration.ofMinutes(1)),
      HttpHeadersConfig.headers(c, V2_3)
    );
  }
}
