package org.opentripplanner.standalone.config.routerconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_0;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_4;

import java.time.Duration;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class ServerConfig {

  private final String requestLogFile;
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

    this.requestLogFile =
      c
        .of("requestLogFile")
        .since(V2_0)
        .summary("The path of the log file for the requests.")
        .description(
          """
You can log some characteristics of trip planning requests in a file for later analysis. Some
transit agencies and operators find this information useful for identifying existing or unmet
transportation demand. Logging will be performed only if you specify a log file name in the router
config.

Each line in the resulting log file will look like this:

```
2016-04-19T18:23:13.486 0:0:0:0:0:0:0:1 ARRIVE 2016-04-07T00:17 WALK,BUS,CABLE_CAR,TRANSIT,BUSISH 45.559737193889966 -122.64999389648438 45.525592487765635 -122.39044189453124 6095 3 5864 3 6215 3
```

The fields separated by whitespace are (in order):

1. Date and time the request was received
2. IP address of the user
3. Arrive or depart search
4. The arrival or departure time
5. A comma-separated list of all transport modes selected
6. Origin latitude and longitude
7. Destination latitude and longitude

Finally, for each itinerary returned to the user, there is a travel duration in seconds and the
number of transit vehicles used in that itinerary.
          """
        )
        .asString(null);

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

  public String requestLogFile() {
    return requestLogFile;
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
