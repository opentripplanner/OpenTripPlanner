package org.opentripplanner.transit.raptor.speed_test.options;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitTuningParameters;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.standalone.config.TransitRoutingConfig;
import org.opentripplanner.transit.raptor.api.request.RaptorTuningParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;

import static org.opentripplanner.standalone.config.RoutingRequestMapper.mapRoutingRequest;

public class SpeedTestConfig {

    private static final Logger LOG = LoggerFactory.getLogger(SpeedTestConfig.class);
    public static final String FILE_NAME = "speed-test-config.json";

    private final JsonNode rawNode;

    /**
     * The test date is the date used for all test cases. The default value is today.
     */
    public final LocalDate testDate;

    public final int maxWalkDistanceMeters;
    public final double walkSpeedMeterPrSecond;
    public final TransitRoutingConfig transitRoutingParams;
    public final RoutingRequest request;

    public SpeedTestConfig(JsonNode node) {
        NodeAdapter adapter = new NodeAdapter(node, FILE_NAME);
        this.rawNode = node;
        testDate = adapter.asDateOrRelativePeriod("testDate", "PT0D");
        maxWalkDistanceMeters = adapter.asInt("maxWalkDistanceMeters", 1000);
        walkSpeedMeterPrSecond = adapter.asDouble("walkSpeedMeterPrSecond", 1.4);
        transitRoutingParams = new TransitRoutingConfig(adapter.path("tuningParameters"));
        request = mapRoutingRequest(adapter.path("routingDefaults"));
    }

    @Override
    public String toString() {
        return rawNode.toPrettyString();
    }

    public RaptorTuningParameters raptorTuningParameters() {
        return transitRoutingParams;
    }

    public TransitTuningParameters transitTuningParameters() {
        return transitRoutingParams;
    }

    public static SpeedTestConfig config(File dir) {
        try {
            File configFile = new File(dir, FILE_NAME);

            if(!configFile.exists()) {
                LOG.warn(
                        "SpeedTest config file not found. Default " +
                        "config is used. Missing file: {}",
                        configFile.getAbsolutePath()
                );
                return new SpeedTestConfig(MissingNode.getInstance());
            }

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
            mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

            SpeedTestConfig config = new SpeedTestConfig(mapper.readTree(configFile));
            LOG.info("SpeedTest config loaded: " + config);
            return config;
        }
        catch (IOException e) {
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }
    }
}
