package org.opentripplanner.standalone.config.feed;

import java.net.URI;
import org.opentripplanner.standalone.config.NodeAdapter;

public class DemExtractConfigBuilder {

  private URI source;
  private Double elevationUnitMultiplier;

  public static DemExtractConfigBuilder of(NodeAdapter config) {
    DemExtractConfigBuilder demExtractConfigBuilder = new DemExtractConfigBuilder();
    demExtractConfigBuilder.source = config.asUri("source");
    demExtractConfigBuilder.elevationUnitMultiplier =
      config.asDoubleOptional("elevationUnitMultiplier").orElse(null);
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

  public Double getElevationUnitMultiplier() {
    return elevationUnitMultiplier;
  }
}
