package org.opentripplanner.transit.raptor.speed_test.options;

import static org.opentripplanner.standalone.config.RoutingRequestMapper.mapRoutingRequest;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.net.URI;
import java.time.LocalDate;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.standalone.config.ConfigLoader;
import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.standalone.config.TransitRoutingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpeedTestConfig {

  private static final Logger LOG = LoggerFactory.getLogger(SpeedTestConfig.class);
  public static final String FILE_NAME = "speed-test-config.json";

  private final JsonNode rawNode;

  /**
   * The test date is the date used for all test cases. The default value is today.
   */
  public final LocalDate testDate;

  /** The speed test run all its test on an existing pre-build graph. */
  public final URI graph;

  public final TransitRoutingConfig transitRoutingParams;
  public final RoutingRequest request;

  public SpeedTestConfig(JsonNode node) {
    NodeAdapter adapter = new NodeAdapter(node, FILE_NAME);
    this.rawNode = node;
    testDate = adapter.asDateOrRelativePeriod("testDate", "PT0D");
    graph = adapter.asUri("graph", null);
    transitRoutingParams = new TransitRoutingConfig(adapter.path("tuningParameters"));
    request = mapRoutingRequest(adapter.path("routingDefaults"));
  }

  public static SpeedTestConfig config(File dir) {
    var json = new ConfigLoader(dir).loadJsonByFilename(FILE_NAME);
    SpeedTestConfig config = new SpeedTestConfig(json);
    LOG.info("SpeedTest config loaded: " + config);
    return config;
  }

  @Override
  public String toString() {
    return rawNode.toPrettyString();
  }
}
