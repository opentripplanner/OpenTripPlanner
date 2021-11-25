package org.opentripplanner.standalone.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.opentripplanner.ext.flex.FlexParameters;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitTuningParameters;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.standalone.config.sandbox.FlexConfig;
import org.opentripplanner.transit.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.updater.UpdatersParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

import static org.opentripplanner.standalone.config.RoutingRequestMapper.mapRoutingRequest;

/**
 * This class is an object representation of the 'router-config.json'.
 */
public class RouterConfig implements Serializable {

    private static final double DEFAULT_STREET_ROUTING_TIMEOUT = 5.0;
    private static final Logger LOG = LoggerFactory.getLogger(RouterConfig.class);

    public static final RouterConfig DEFAULT = new RouterConfig(
            MissingNode.getInstance(), "DEFAULT", false
    );

    /**
     * The raw JsonNode three kept for reference and (de)serialization.
     */
    private final JsonNode rawJson;
    private final String configVersion;
    private final String requestLogFile;
    private final boolean transmodelApiHideFeedId;
    private final double streetRoutingTimeoutSeconds;
    private final RoutingRequest routingRequestDefaults;
    private final TransitRoutingConfig transitConfig;
    private final UpdatersParameters updatersParameters;
    private final VectorTileConfig vectorTileLayers;
    private final FlexConfig flexConfig;

    public RouterConfig(JsonNode node, String source, boolean logUnusedParams) {
        NodeAdapter adapter = new NodeAdapter(node, source);
        this.rawJson = node;
        this.configVersion = adapter.asText("configVersion", null);
        this.requestLogFile = adapter.asText("requestLogFile", null);
        this.transmodelApiHideFeedId = adapter.path("transmodelApi").asBoolean("hideFeedId", false);
        this.streetRoutingTimeoutSeconds = adapter.asDouble(
                "streetRoutingTimeout", DEFAULT_STREET_ROUTING_TIMEOUT
        );
        this.transitConfig = new TransitRoutingConfig(adapter.path("transit"));
        this.routingRequestDefaults = mapRoutingRequest(adapter.path("routingDefaults"));
        this.updatersParameters = new UpdatersConfig(adapter);
        this.vectorTileLayers = new VectorTileConfig(adapter.path("vectorTileLayers").asList());
        this.flexConfig = new FlexConfig(adapter.path("flex"));

        if(logUnusedParams) {
            adapter.logAllUnusedParameters(LOG);
        }
    }

    /**
     * The config-version is a parameter witch each OTP deployment may set to be able to
     * query the OTP server and verify that it uses the correct version of the config. The
     * version must be injected into the config in the operation deployment pipeline. How this
     * is done is up to the deployment.
     * <p>
     * The config-version have no effect on OTP, and is provided as is on the API. There is
     * not syntax or format check on the version and it can be any string.
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
     * The preferred way to limit the search is to limit the distance for
     * each street mode(WALK, BIKE, CAR). So the default timeout for a
     * street search is set quite high. This is used to abort the search
     * if the max distance is not reached within the timeout.
     */
    public double streetRoutingTimeoutSeconds() {
        return streetRoutingTimeoutSeconds;
    }

    public boolean transmodelApiHideFeedId() { return transmodelApiHideFeedId; }

    public RoutingRequest routingRequestDefaults() {
        return routingRequestDefaults;
    }

    public RaptorTuningParameters raptorTuningParameters() {
        return transitConfig;
    }

    public TransitTuningParameters transitTuningParameters() {
        return transitConfig;
    }

    public UpdatersParameters updaterConfig() { return updatersParameters; }

    public VectorTilesResource.LayersParameters vectorTileLayers() { return vectorTileLayers; }

    public FlexParameters flexParameters(RoutingRequest request) { 
        return flexConfig.toFlexParameters(request);
    }

    /**
     * If {@code true} the config is loaded from file, in not the DEFAULT config is used.
     */
    public boolean isDefault() {
        return this.rawJson.isMissingNode();
    }

    public String toJson() {
        return rawJson.isMissingNode() ? "" : rawJson.toString();
    }

    public String toString() {
        // Print ONLY the values set, not deafult values
        return rawJson.toPrettyString();
    }

}
