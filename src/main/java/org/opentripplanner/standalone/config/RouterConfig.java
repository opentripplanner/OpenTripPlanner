package org.opentripplanner.standalone.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitTuningParameters;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.transit.raptor.api.request.RaptorTuningParameters;
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
    public final JsonNode rawJson;

    private final String requestLogFile;
    private final double streetRoutingTimeoutSeconds;
    private final RoutingRequest routingRequestDefaults;
    private final TransitRoutingConfig transitConfig;

    public RouterConfig(JsonNode node, String source, boolean logUnusedParams) {
        NodeAdapter adapter = new NodeAdapter(node, source);
        this.rawJson = node;
        this.requestLogFile = adapter.asText("requestLogFile", null);
        this.streetRoutingTimeoutSeconds = adapter.asDouble(
                "streetRoutingTimeout", DEFAULT_STREET_ROUTING_TIMEOUT
        );
        this.transitConfig = new TransitRoutingConfig(adapter.path("transit"));
        this.routingRequestDefaults = mapRoutingRequest(adapter.path("routingDefaults"));

        // Touch parameters read at a later point in time to avoid reporting them when
        // logging unused parameters.
        adapter.path("updaters");

        if(logUnusedParams) {
            adapter.logAllUnusedParameters(LOG);
        }
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

    public RoutingRequest routingRequestDefaults() {
        return routingRequestDefaults;
    }

    public RaptorTuningParameters raptorTuningParameters() {
        return transitConfig;
    }

    public TransitTuningParameters transitTuningParameters() {
        return transitConfig;
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
