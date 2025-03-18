package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_0;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_3;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_7;

import java.time.Duration;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.trip.siri.updater.SiriETUpdaterParameters;

public class SiriETUpdaterConfig {

  public static SiriETUpdaterParameters create(String configRef, NodeAdapter c) {
    return new SiriETUpdaterParameters(
      configRef,
      c.of("feedId").since(V2_0).summary("The ID of the feed to apply the updates to.").asString(),
      c
        .of("blockReadinessUntilInitialized")
        .since(V2_0)
        .summary(
          "Whether catching up with the updates should block the readiness check from returning a 'ready' result."
        )
        .asBoolean(false),
      c
        .of("url")
        .since(V2_0)
        .summary("The URL to send the HTTP requests to.")
        .description(SiriSXUpdaterConfig.URL_DESCRIPTION)
        .asString(),
      c
        .of("frequency")
        .since(V2_0)
        .summary("How often the updates should be retrieved.")
        .asDuration(Duration.ofMinutes(1)),
      c.of("requestorRef").since(V2_0).summary("The requester reference.").asString(null),
      c
        .of("timeout")
        .since(V2_0)
        .summary("The HTTP timeout to download the updates.")
        .asDuration(Duration.ofSeconds(15)),
      c.of("previewInterval").since(V2_0).summary("TODO").asDuration(null),
      c
        .of("fuzzyTripMatching")
        .since(V2_0)
        .summary("If the fuzzy trip matcher should be used to match trips.")
        .asBoolean(false),
      HttpHeadersConfig.headers(c, V2_3),
      c
        .of("producerMetrics")
        .since(V2_7)
        .summary("If failure, success, and warning metrics should be collected per producer.")
        .asBoolean(false)
    );
  }
}
