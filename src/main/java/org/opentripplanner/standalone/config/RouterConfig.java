package org.opentripplanner.standalone.config;

import static org.opentripplanner.framework.application.OtpFileNames.ROUTER_CONFIG_FILENAME;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_0;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_1;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import org.opentripplanner.RideHailingServicesParameters;
import org.opentripplanner.ext.ridehailing.RideHailingServiceParameters;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.routerconfig.RideHailingServicesConfig;
import org.opentripplanner.standalone.config.routerconfig.TransitRoutingConfig;
import org.opentripplanner.standalone.config.routerconfig.UpdatersConfig;
import org.opentripplanner.standalone.config.routerconfig.VectorTileConfig;
import org.opentripplanner.standalone.config.routerequest.RouteRequestConfig;
import org.opentripplanner.standalone.config.sandbox.FlexConfig;
import org.opentripplanner.standalone.config.sandbox.TransmodelAPIConfig;
import org.opentripplanner.updater.UpdatersParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is an object representation of the 'router-config.json'.
 */
public class RouterConfig implements Serializable {

  private static final Duration DEFAULT_STREET_ROUTING_TIMEOUT = Duration.ofSeconds(5);
  private static final Logger LOG = LoggerFactory.getLogger(RouterConfig.class);

  public static final RouterConfig DEFAULT = new RouterConfig(
    MissingNode.getInstance(),
    "DEFAULT",
    false
  );

  /**
   * The node adaptor kept for reference and (de)serialization.
   */
  private final NodeAdapter root;
  private final String configVersion;
  private final String requestLogFile;
  private final TransmodelAPIConfig transmodelApi;
  private final Duration streetRoutingTimeout;
  private final RouteRequest routingRequestDefaults;
  private final TransitRoutingConfig transitConfig;
  private final UpdatersParameters updatersParameters;
  private final RideHailingServicesParameters rideHailingServiceParameters;
  private final VectorTileConfig vectorTileLayers;
  private final FlexConfig flexConfig;

  public RouterConfig(JsonNode node, String source, boolean logUnusedParams) {
    this(new NodeAdapter(node, source), logUnusedParams);
  }

  /** protected to give unit-test access */
  RouterConfig(NodeAdapter root, boolean logUnusedParams) {
    this.root = root;
    this.configVersion =
      root
        .of("configVersion")
        .since(V2_1)
        .summary("Deployment version of the *" + ROUTER_CONFIG_FILENAME + "*.")
        .description(OtpConfig.CONFIG_VERSION_DESCRIPTION)
        .asString(null);
    this.requestLogFile =
      root
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
    this.transmodelApi =
      new TransmodelAPIConfig(
        root
          .of("transmodelApi")
          .since(V2_1)
          .summary("Configuration for the Transmodel GraphQL API.")
          .asObject()
      );
    this.streetRoutingTimeout = parseStreetRoutingTimeout(root);
    this.transitConfig = new TransitRoutingConfig("transit", root);
    this.routingRequestDefaults =
      RouteRequestConfig.mapDefaultRouteRequest(root, "routingDefaults");
    this.updatersParameters = new UpdatersConfig(root);
    this.rideHailingServiceParameters = new RideHailingServicesConfig(root);
    this.vectorTileLayers = VectorTileConfig.mapVectorTilesParameters(root, "vectorTileLayers");
    this.flexConfig = new FlexConfig(root, "flex");

    if (logUnusedParams && LOG.isWarnEnabled()) {
      root.logAllUnusedParameters(LOG::warn);
    }
  }

  /**
   * The config-version is a parameter which each OTP deployment may set to be able to query the OTP
   * server and verify that it uses the correct version of the config. The version must be injected
   * into the config in the operation deployment pipeline. How this is done is up to the
   * deployment.
   * <p>
   * The config-version have no effect on OTP, and is provided as is on the API. There is not syntax
   * or format check on the version and it can be any string.
   * <p>
   * Be aware that OTP uses the config embedded in the loaded graph if no new config is provided.
   * <p>
   * This parameter is optional, and the default is {@code null}.
   */
  public String getConfigVersion() {
    return configVersion;
  }

  public String requestLogFile() {
    return requestLogFile;
  }

  /**
   * The preferred way to limit the search is to limit the distance for each street mode(WALK, BIKE,
   * CAR). So the default timeout for a street search is set quite high. This is used to abort the
   * search if the max distance is not reached within the timeout.
   */
  public Duration streetRoutingTimeout() {
    return streetRoutingTimeout;
  }

  public TransmodelAPIConfig transmodelApi() {
    return transmodelApi;
  }

  public RouteRequest routingRequestDefaults() {
    return routingRequestDefaults;
  }

  public TransitRoutingConfig transitTuningConfig() {
    return transitConfig;
  }

  public UpdatersParameters updaterConfig() {
    return updatersParameters;
  }

  public List<RideHailingServiceParameters> rideHailingServiceParameters() {
    return rideHailingServiceParameters.rideHailingServiceParameters();
  }

  public VectorTilesResource.LayersParameters<VectorTilesResource.LayerType> vectorTileLayers() {
    return vectorTileLayers;
  }

  public FlexConfig flexConfig() {
    return flexConfig;
  }

  public NodeAdapter asNodeAdapter() {
    return root;
  }

  /**
   * If {@code true} the config is loaded from file, in not the DEFAULT config is used.
   */
  public boolean isDefault() {
    return root.isEmpty();
  }

  public String toJson() {
    return root.isEmpty() ? "" : root.toJson();
  }

  public String toString() {
    // Print ONLY the values set, not default values
    return root.toPrettyString();
  }

  /**
   * This method is needed, because we want to support the old format for the "streetRoutingTimeout"
   * parameter. We will keep it for some time, to let OTP deployments update the config.
   *
   * @since 2.2 - The support for the old format can be removed in version > 2.2.
   */
  static Duration parseStreetRoutingTimeout(NodeAdapter adapter) {
    return adapter
      .of("streetRoutingTimeout")
      .since(V2_2)
      .summary(
        "The maximum time a street routing request is allowed to take before returning a timeout."
      )
      .description(
        """
In OTP1 path searches sometimes took a long time to complete. With the new Raptor algorithm this is not
the case anymore. The street part of the routing may still take a long time if searching very long
distances. You can set the street routing timeout to avoid tying up server resources on pointless
searches and ensure that your users receive a timely response. You can also limit the max distance
to search for WALK, BIKE and CAR. When a search times out, a WARN level log entry is made with
information that can help identify problematic searches and improve our routing methods. There are
no timeouts for the transit part of the routing search, instead configure a reasonable dynamic
search-window.

The search aborts after this duration and any paths found are returned to the client.
"""
      )
      .asDuration(DEFAULT_STREET_ROUTING_TIMEOUT);
  }
}
