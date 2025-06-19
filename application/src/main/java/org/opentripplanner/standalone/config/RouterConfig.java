package org.opentripplanner.standalone.config;

import static org.opentripplanner.framework.application.OtpFileNames.ROUTER_CONFIG_FILENAME;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_1;
import static org.opentripplanner.standalone.config.routerequest.RouteRequestConfig.mapDefaultRouteRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import java.io.Serializable;
import java.util.List;
import org.opentripplanner.ext.flex.FlexParameters;
import org.opentripplanner.ext.ridehailing.RideHailingServiceParameters;
import org.opentripplanner.ext.trias.config.TriasApiConfig;
import org.opentripplanner.ext.trias.parameters.TriasApiParameters;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.routerconfig.RideHailingServicesConfig;
import org.opentripplanner.standalone.config.routerconfig.ServerConfig;
import org.opentripplanner.standalone.config.routerconfig.TransitRoutingConfig;
import org.opentripplanner.standalone.config.routerconfig.UpdatersConfig;
import org.opentripplanner.standalone.config.routerconfig.VectorTileConfig;
import org.opentripplanner.standalone.config.sandbox.FlexConfig;
import org.opentripplanner.standalone.config.sandbox.TransmodelAPIConfig;
import org.opentripplanner.updater.UpdatersParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is an object representation of the 'router-config.json'.
 */
public class RouterConfig implements Serializable {

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
  private final ServerConfig server;
  private final RouteRequest routingRequestDefaults;
  private final TransitRoutingConfig transitConfig;
  private final UpdatersParameters updatersParameters;
  private final RideHailingServicesConfig rideHailingConfig;
  private final FlexConfig flexConfig;
  private final TransmodelAPIConfig transmodelApi;
  private final VectorTileConfig vectorTileConfig;
  private final TriasApiParameters triasApiParameters;

  public RouterConfig(JsonNode node, String source, boolean logUnusedParams) {
    this(new NodeAdapter(node, source), logUnusedParams);
  }

  /** protected to give unit-test access */
  RouterConfig(NodeAdapter root, boolean logUnusedParams) {
    this.root = root;
    this.configVersion = root
      .of("configVersion")
      .since(V2_1)
      .summary("Deployment version of the *" + ROUTER_CONFIG_FILENAME + "*.")
      .description(OtpConfig.CONFIG_VERSION_DESCRIPTION)
      .asString(null);

    this.server = new ServerConfig("server", root);
    this.transmodelApi = new TransmodelAPIConfig("transmodelApi", root);
    var request = mapDefaultRouteRequest("routingDefaults", root);
    this.transitConfig = new TransitRoutingConfig("transit", root, request);
    this.routingRequestDefaults = request
      .copyOf()
      .withMaxSearchWindow(transitConfig.maxSearchWindow())
      .buildDefault();
    this.updatersParameters = new UpdatersConfig(root);
    this.rideHailingConfig = new RideHailingServicesConfig(root);
    this.vectorTileConfig = VectorTileConfig.mapVectorTilesParameters(root, "vectorTiles");
    this.triasApiParameters = TriasApiConfig.mapParameters("triasApi", root);
    this.flexConfig = new FlexConfig(root, "flex");

    if (logUnusedParams && LOG.isWarnEnabled()) {
      root.logAllWarnings(LOG::warn);
    }

    // Semantic validation
    server.validate(routingRequestDefaults.preferences().street().routingTimeout());
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

  public ServerConfig server() {
    return server;
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
    return rideHailingConfig.rideHailingServiceParameters();
  }

  public VectorTileConfig vectorTileConfig() {
    return vectorTileConfig;
  }

  public FlexParameters flexParameters() {
    return flexConfig;
  }

  public TriasApiParameters triasApiParameters() {
    return triasApiParameters;
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
   * Checks if any unknown or invalid parameters were encountered while loading the configuration.
   */
  public boolean hasUnknownParameters() {
    return root.hasUnknownParameters();
  }
}
