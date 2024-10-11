package org.opentripplanner.graph_builder.module.ned.parameter;

import java.net.URI;

public class DemExtractParametersBuilder {

  private URI source;
  private Double elevationUnitMultiplier;

  URI source() {
    return source;
  }

  public DemExtractParametersBuilder() {
    this.elevationUnitMultiplier = DemExtractParameters.DEFAULT_ELEVATION_UNIT_MULTIPLIER;
  }

  public DemExtractParametersBuilder(DemExtractParameters original) {
    this.elevationUnitMultiplier = original.elevationUnitMultiplier();
  }

  public DemExtractParametersBuilder withSource(URI source) {
    this.source = source;
    return this;
  }

  Double elevationUnitMultiplier() {
    return elevationUnitMultiplier;
  }

  public DemExtractParametersBuilder withElevationUnitMultiplier(Double elevationUnitMultiplier) {
    this.elevationUnitMultiplier = elevationUnitMultiplier;
    return this;
  }

  public DemExtractParameters build() {
    return new DemExtractParameters(this);
  }
}
