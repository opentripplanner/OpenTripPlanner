package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_0;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_3;

import org.opentripplanner.ext.siri.updater.SiriETUpdaterParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

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
      c.of("url").since(V2_0).summary("The URL to send the HTTP requests to.").asString(),
      c
        .of("frequencySec")
        .since(V2_0)
        .summary("How often the updates should be retrieved.")
        .asInt(60),
      c.of("requestorRef").since(V2_0).summary("The requester reference.").asString(null),
      c.of("timeoutSec").since(V2_0).summary("The HTTP timeout to download the updates.").asInt(15),
      c.of("previewIntervalMinutes").since(V2_0).summary("TODO").asInt(-1),
      c
        .of("fuzzyTripMatching")
        .since(V2_0)
        .summary("If the fuzzy trip matcher should be used to match trips.")
        .asBoolean(false),
      c.of("headers").since(V2_3).summary("HTTP headers to add to the request").asStringMap()
    );
  }
}
