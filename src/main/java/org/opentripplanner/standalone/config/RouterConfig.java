package org.opentripplanner.standalone.config;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;
import static org.opentripplanner.standalone.config.routingrequest.RoutingRequestMapper.mapRoutingRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import java.io.Serializable;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.opentripplanner.ext.flex.FlexParameters;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParameters;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.routerconfig.TransitRoutingConfig;
import org.opentripplanner.standalone.config.routerconfig.UpdatersConfig;
import org.opentripplanner.standalone.config.routerconfig.VectorTileConfig;
import org.opentripplanner.standalone.config.sandbox.FlexConfig;
import org.opentripplanner.standalone.config.sandbox.TransmodelAPIConfig;
import org.opentripplanner.transit.raptor.api.request.RaptorTuningParameters;
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
  private final VectorTileConfig vectorTileLayers;
  private final FlexConfig flexConfig;

  public RouterConfig(JsonNode node, String source, boolean logUnusedParams) {
    this(new NodeAdapter(node, source), logUnusedParams);
  }

  /** protected to give unit-test access */
  RouterConfig(NodeAdapter root, boolean logUnusedParams) {
    this.root = root;
    this.configVersion = root.of("configVersion").withDoc(NA, /*TODO DOC*/"TODO").asString(null);
    this.requestLogFile = root.of("requestLogFile").withDoc(NA, /*TODO DOC*/"TODO").asString(null);
    this.transmodelApi =
      new TransmodelAPIConfig(
        root
          .of("transmodelApi")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .withDescription(/*TODO DOC*/"TODO")
          .asObject()
      );
    this.streetRoutingTimeout = parseStreetRoutingTimeout(root);
    this.transitConfig =
      new TransitRoutingConfig(
        root
          .of("transit")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .withDescription(/*TODO DOC*/"TODO")
          .asObject()
      );
    this.routingRequestDefaults =
      mapRoutingRequest(
        root
          .of("routingDefaults")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .withDescription(/*TODO DOC*/"TODO")
          .asObject()
      );
    this.updatersParameters = new UpdatersConfig(root);
    this.vectorTileLayers = VectorTileConfig.mapVectorTilesParameters(root, "vectorTileLayers");
    this.flexConfig =
      new FlexConfig(
        root
          .of("flex")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .withDescription(/*TODO DOC*/"TODO")
          .asObject()
      );

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

  public RaptorTuningParameters raptorTuningParameters() {
    return transitConfig;
  }

  public TransitTuningParameters transitTuningParameters() {
    return transitConfig;
  }

  public UpdatersParameters updaterConfig() {
    return updatersParameters;
  }

  public VectorTilesResource.LayersParameters vectorTileLayers() {
    return vectorTileLayers;
  }

  public FlexParameters flexParameters(RoutingPreferences preferences) {
    return flexConfig.toFlexParameters(preferences);
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
   * @since 2.2 - The support for the old format can be removed in version > 2.2.
   */
  static Duration parseStreetRoutingTimeout(NodeAdapter adapter) {
    return adapter
      .of("streetRoutingTimeout")
      .withDoc(NA, /*TODO DOC*/"TODO")
      .asDuration2(DEFAULT_STREET_ROUTING_TIMEOUT, ChronoUnit.SECONDS);
  }
}
