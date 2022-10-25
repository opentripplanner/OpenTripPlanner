package org.opentripplanner.transit.raptor.speed_test.options;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;
import static org.opentripplanner.standalone.config.routingrequest.RoutingRequestMapper.mapRoutingRequest;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.standalone.config.framework.file.ConfigFileLoader;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.routerconfig.TransitRoutingConfig;
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

  public final String feedId;

  public final TransitRoutingConfig transitRoutingParams;
  public final RouteRequest request;

  public SpeedTestConfig(JsonNode node) {
    NodeAdapter adapter = new NodeAdapter(node, FILE_NAME);
    this.rawNode = node;
    testDate =
      adapter
        .of("testDate")
        .since(NA)
        .summary("TODO")
        .asDateOrRelativePeriod("PT0D", ZoneId.of("UTC"));
    graph = adapter.of("graph").since(NA).summary("TODO").asUri(null);
    feedId = adapter.of("feedId").since(NA).summary("TODO").asString();
    transitRoutingParams =
      new TransitRoutingConfig(
        adapter
          .of("tuningParameters")
          .since(NA)
          .summary("TODO")
          .description(/*TODO DOC*/"TODO")
          .asObject()
      );
    request =
      mapRoutingRequest(
        adapter
          .of("routingDefaults")
          .since(NA)
          .summary("TODO")
          .description(/*TODO DOC*/"TODO")
          .asObject()
      );
  }

  public static SpeedTestConfig config(File dir) {
    var json = ConfigFileLoader.of().withConfigDir(dir).loadFromFile(FILE_NAME);
    SpeedTestConfig config = new SpeedTestConfig(json);
    LOG.info("SpeedTest config loaded: {}", config);
    return config;
  }

  @Override
  public String toString() {
    return rawNode.toPrettyString();
  }
}
