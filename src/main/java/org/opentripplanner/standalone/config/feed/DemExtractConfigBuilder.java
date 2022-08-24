package org.opentripplanner.standalone.config.feed;

import java.net.URI;
import org.opentripplanner.standalone.config.NodeAdapter;

public class DemExtractConfigBuilder {

  private URI source;
  private double elevationUnitMultiplier = 1.0;

  public static DemExtractConfigBuilder of(NodeAdapter config) {
    DemExtractConfigBuilder demExtractConfigBuilder = new DemExtractConfigBuilder();
    demExtractConfigBuilder.source = config.asUri("source");
    demExtractConfigBuilder.elevationUnitMultiplier =
      config.asDouble("elevationUnitMultiplier", 1.0);
    return demExtractConfigBuilder;
  }

  public DemExtractConfigBuilder withSource(URI source) {
    this.source = source;
    return this;
  }

  public DemExtractConfig build() {
    return new DemExtractConfig(this);
  }

  public URI getSource() {
    return source;
  }

  public double getElevationUnitMultiplier() {
    return elevationUnitMultiplier;
  }
}
