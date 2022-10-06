package org.opentripplanner.graph_builder.module.ned.parameter;

import java.net.URI;

public class DemExtractConfigBuilder {

  private URI source;
  private Double elevationUnitMultiplier;

  URI source() {
    return source;
  }

  public DemExtractConfigBuilder withSource(URI source) {
    this.source = source;
    return this;
  }

  Double elevationUnitMultiplier() {
    return elevationUnitMultiplier;
  }

  public DemExtractConfigBuilder withElevationUnitMultiplier(Double elevationUnitMultiplier) {
    this.elevationUnitMultiplier = elevationUnitMultiplier;
    return this;
  }

  public DemExtractParameters build() {
    return new DemExtractParameters(this);
  }
}
