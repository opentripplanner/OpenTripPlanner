package org.opentripplanner.standalone.config.routerconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_4;

import java.time.Duration;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class ServerConfig {

  private final Duration apiProcessingTimeout;

  public ServerConfig(String parameterName, NodeAdapter root) {
    NodeAdapter c = root
      .of(parameterName)
      .since(V2_4)
      .summary("Configuration for router server.")
      .description(
        """
These parameters are used to configure the router server. Many parameters are specific to a 
domain, these are set tin the routing request.
        """
      )
      .asObject();

    this.apiProcessingTimeout =
      c
        .of("apiProcessingTimeout")
        .since(V2_4)
        .summary("Maximum processing time for an API request")
        .description(
          """
       This timeout limits the server-side processing time for a given API request.
       This does not include network latency nor waiting time in the HTTP server thread pool.
       The default value is `-1s` (no timeout).
       The timeout is applied to all APIs (REST, Transmodel , Legacy GraphQL).
        """
        )
        .asDuration(Duration.ofSeconds(-1));
  }

  public Duration apiProcessingTimeout() {
    return apiProcessingTimeout;
  }

  public void validate(Duration streetRoutingTimeout) {
    if (
      !apiProcessingTimeout.isNegative() &&
      streetRoutingTimeout.toSeconds() > apiProcessingTimeout.toSeconds()
    ) {
      throw new OtpAppException(
        "streetRoutingTimeout (" +
        streetRoutingTimeout +
        ") must be shorter than apiProcessingTimeout (" +
        apiProcessingTimeout +
        ')'
      );
    }
  }
}
