package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_0;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_7;

import java.time.Duration;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.alert.siri.lite.SiriSXLiteUpdaterParameters;

public class SiriSXLiteUpdaterConfig {

  public static SiriSXLiteUpdaterParameters create(String configRef, NodeAdapter c) {
    return new SiriSXLiteUpdaterParameters(
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
        .of("earlyStart")
        .since(V2_0)
        .summary("This value is subtracted from the actual validity defined in the message.")
        .description(
          """
          Normally the planned departure time is used, so setting this to 10s will cause the
          SX-message to be included in trip-results 10 seconds before the the planned departure
          time."""
        )
        .asDuration(Duration.ZERO),
      HttpHeadersConfig.headers(c, V2_7)
    );
  }
}
