package org.opentripplanner.standalone.config;

import java.net.URI;

public class DemExtractConfig {

  public final URI source;
  public final double elevationUnitMultiplier;

  DemExtractConfig(NodeAdapter config) {
    source = config.asUri("source");
    elevationUnitMultiplier = config.asDouble("elevationUnitMultiplier", 1.0);
  }
}
