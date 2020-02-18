package org.opentripplanner.standalone.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.opentripplanner.reflect.ReflectiveInitializer;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.transit.raptor.api.request.RaptorTuningParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * This class is an object representation of the 'router-config.json'.
 */
public class RouterConfigParams implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(RouterConfigParams.class);

    public static final RouterConfigParams DEFAULT = new RouterConfigParams(
            MissingNode.getInstance(), "DEFAULT"
    );

    /**
     * The raw JsonNode three kept for reference and (de)serialization.
     */
    public final JsonNode rawJson;
    public final String requestLogFile;
    public final RaptorTuningParameters raptorTuningParameters;

    // TODO OTP2 - Add routing parameters here

    public RouterConfigParams(JsonNode node, String source) {
        NodeAdapter adapter = new NodeAdapter(node, source);
        this.rawJson = adapter.asRawNode();
        this.requestLogFile = adapter.asText("requestLogFile", null);
        this.raptorTuningParameters = new TransitTuningParameters(adapter.path("transit"));
    }

    public RoutingRequest routingDefaults() {
        /* Create the default router parameters from the JSON router config. */
        JsonNode routingDefaultsNode = rawJson.get("routingDefaults");

        if (routingDefaultsNode != null) {
            LOG.info("Loading default routing parameters from JSON:");
            ReflectiveInitializer<RoutingRequest> scraper = new ReflectiveInitializer<>(RoutingRequest.class);
            return scraper.scrape(routingDefaultsNode);
        } else {
            LOG.info("No default routing parameters were found in the router config JSON. Using built-in OTP defaults.");
            return new RoutingRequest();
        }
    }

    public double[] timeouts(double[] defaultValue) {
        /* Apply multiple timeouts. */
        JsonNode timeouts = rawJson.get("timeouts");
        if (timeouts != null) {
            if (timeouts.isArray() && timeouts.size() > 0) {
                double[] array = new double[timeouts.size()];
                int i = 0;
                for (JsonNode node : timeouts) {
                    array[i++] = node.doubleValue();
                }
                return array;
            } else {
                LOG.error("The 'timeouts' configuration option should be an array of values in seconds.");
            }
        }
        /* Apply single timeout, if multiple timeouts not found. */
        JsonNode timeout = rawJson.get("timeout");
        if (timeout != null) {
            if (timeout.isNumber()) {
                return new double[]{timeout.doubleValue()};
            } else {
                LOG.error("The 'timeout' configuration option should be a number of seconds.");
            }
        }
        return defaultValue;
    }

    public Map<TraverseMode, Integer> boardTimes() {
        return getTraverseModeTimes("boardTimes");
    }

    public Map<TraverseMode, Integer> alightTimes() {
        return getTraverseModeTimes("alightTimes");
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

    private Map<TraverseMode, Integer> getTraverseModeTimes(String key) {
        JsonNode boardTimes = rawJson.get(key);
        if (boardTimes == null || !boardTimes.isObject()) {
            return Collections.emptyMap();
        }
        Map<TraverseMode, Integer> times = new EnumMap<>(TraverseMode.class);
        for (TraverseMode mode : TraverseMode.values()) {
            if (boardTimes.has(mode.name())) {
                times.put(mode, boardTimes.get(mode.name()).asInt(0));
            }
        }
        return times;
    }
}
