package org.opentripplanner.transit.speed_test.options;

import java.io.File;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import org.opentripplanner.standalone.config.framework.file.ConfigFileLoader;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.utils.tostring.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpeedTestConfig {

  private static final Logger LOG = LoggerFactory.getLogger(SpeedTestConfig.class);
  private static final String FILE_NAME = "speed-test-config.json";
  private static final SpeedTestConfig DEFAULT = new SpeedTestConfig();

  private final String feedId;
  private final LocalDate testDate;
  private final boolean ignoreStreetResults;
  private final URI graph;

  public SpeedTestConfig() {
    this.feedId = "F";
    this.testDate = null;
    this.ignoreStreetResults = true;
    this.graph = URI.create("graph.obj");
  }

  public SpeedTestConfig(Builder builder) {
    this.feedId = Objects.requireNonNull(builder.feedId);
    this.testDate = Objects.requireNonNull(builder.testDate);
    this.ignoreStreetResults = Objects.requireNonNull(builder.ignoreStreetResults);
    this.graph = Objects.requireNonNull(builder.graph);
  }

  public static SpeedTestConfig.Builder of() {
    return new Builder(DEFAULT);
  }

  public static SpeedTestConfig config(File dir) {
    var fileLoader = ConfigFileLoader.of().withConfigDir(dir);
    var json = fileLoader.loadFromFile(FILE_NAME);
    return SpeedTestConfig.createFromConfig(new NodeAdapter(json, FILE_NAME));
  }

  public SpeedTestConfig.Builder copyOf() {
    return new Builder(this);
  }

  /**
   * Load SpeedTest configuration form the given JSON Adaptor. If a routerConfig is provided
   * that config is used, if not relevant router config nodes are loaded from the   the config is loaded from the
   */
  public static SpeedTestConfig createFromConfig(NodeAdapter adapter) {
    var builder = of()
      .withFeedId(adapter.of("feedId").asString())
      .withTestDate(adapter.of("testDate").asDateOrRelativePeriod("PT0D", ZoneId.of("UTC")))
      .withIgnoreStreetResults(adapter.of("ignoreStreetResults").asBoolean(false))
      .withGraph(adapter.of("graph").asUri(null));

    adapter.logAllWarnings(LOG::warn);

    return builder.build();
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(getClass())
      .addStr("feedId", feedId)
      .addDate("testDate", testDate)
      .addBoolIfTrue("ignoreStreetResults", ignoreStreetResults)
      .addObj("graph", graph)
      .toString();
  }

  public String feedId() {
    return feedId;
  }

  /**
   * The test date is the date used for all test cases. The default value is today.
   */
  public LocalDate testDate() {
    return testDate;
  }

  public boolean ignoreStreetResults() {
    return ignoreStreetResults;
  }

  /** The speed test run all its test on an existing pre-build graph. */
  public URI graph() {
    return graph;
  }

  public static class Builder {

    public String feedId;
    public LocalDate testDate;
    public boolean ignoreStreetResults;
    public URI graph;

    Builder(SpeedTestConfig original) {
      if (original != null) {
        this.feedId = original.feedId;
        this.testDate = original.testDate;
        this.ignoreStreetResults = original.ignoreStreetResults;
        this.graph = original.graph;
      }
    }

    public Builder withFeedId(String feedId) {
      this.feedId = feedId;
      return this;
    }

    public Builder withTestDate(LocalDate testDate) {
      this.testDate = testDate;
      return this;
    }

    public Builder withIgnoreStreetResults(boolean ignoreStreetResults) {
      this.ignoreStreetResults = ignoreStreetResults;
      return this;
    }

    public Builder withGraph(URI graph) {
      this.graph = graph;
      return this;
    }

    public SpeedTestConfig build() {
      return new SpeedTestConfig(this);
    }
  }
}
