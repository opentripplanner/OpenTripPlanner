package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_0;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_3;

import java.time.Duration;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.alert.siri.SiriSXUpdaterParameters;

public class SiriSXUpdaterConfig {

  static final String URL_DESCRIPTION =
    """
    Use the file protocol to set a directory for reading updates from a directory. The file
    loader will look for xml files: '*.xml' in the configured directory. The files are
    renamed by the loader when processed:

    &nbsp;&nbsp;&nbsp; _a.xml_ &nbsp; ➞ &nbsp; _a.xml.inProgress_ &nbsp; ➞ &nbsp; _a.xml.ok_ &nbsp; or &nbsp; _a.xml.failed_

    """;

  public static SiriSXUpdaterParameters create(String configRef, NodeAdapter c) {
    return new SiriSXUpdaterParameters(
      configRef,
      c.of("feedId").since(V2_0).summary("The ID of the feed to apply the updates to.").asString(),
      c
        .of("url")
        .since(V2_0)
        .summary(
          """
          The URL to send the HTTP requests to. Supports http/https and file protocol.
          """
        )
        .description(URL_DESCRIPTION)
        .asString(),
      c.of("requestorRef").since(V2_0).summary("The requester reference.").asString(null),
      c
        .of("frequency")
        .since(V2_0)
        .summary("How often the updates should be retrieved.")
        .asDuration(Duration.ofMinutes(1)),
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
      c
        .of("timeout")
        .since(V2_0)
        .summary("The HTTP timeout to download the updates.")
        .asDuration(Duration.ofSeconds(15)),
      c
        .of("blockReadinessUntilInitialized")
        .since(V2_0)
        .summary(
          "Whether catching up with the updates should block the readiness check from returning a 'ready' result."
        )
        .asBoolean(false),
      HttpHeadersConfig.headers(c, V2_3)
    );
  }
}
