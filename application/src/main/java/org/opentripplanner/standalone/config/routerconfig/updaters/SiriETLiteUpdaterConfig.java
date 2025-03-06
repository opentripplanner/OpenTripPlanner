package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_7;

import java.time.Duration;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.trip.siri.updater.lite.SiriETLiteUpdaterParameters;

public class SiriETLiteUpdaterConfig {

  public static SiriETLiteUpdaterParameters create(String configRef, NodeAdapter c) {
    return new SiriETLiteUpdaterParameters(
      configRef,
      c.of("feedId").since(V2_7).summary("The ID of the feed to apply the updates to.").asString(),
      c
        .of("url")
        .since(V2_7)
        .summary("The URL to send the HTTP requests to.")
        .description(SiriSXUpdaterConfig.URL_DESCRIPTION)
        .asUri(),
      c
        .of("frequency")
        .since(V2_7)
        .summary("How often the updates should be retrieved.")
        .asDuration(Duration.ofMinutes(1)),
      c
        .of("timeout")
        .since(V2_7)
        .summary("The HTTP timeout to download the updates.")
        .asDuration(Duration.ofSeconds(15)),
      c
        .of("fuzzyTripMatching")
        .since(V2_7)
        .summary("If the fuzzy trip matcher should be used to match trips.")
        .asBoolean(false),
      HttpHeadersConfig.headers(c, V2_7)
    );
  }
}
