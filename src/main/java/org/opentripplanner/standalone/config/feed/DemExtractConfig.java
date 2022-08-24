package org.opentripplanner.standalone.config.feed;

import java.net.URI;
import java.util.Objects;

public class DemExtractConfig {

  public final URI source;
  public final double elevationUnitMultiplier;

  DemExtractConfig(DemExtractConfigBuilder demExtractConfigBuilder) {
    source = Objects.requireNonNull(demExtractConfigBuilder.getSource());
    elevationUnitMultiplier = demExtractConfigBuilder.getElevationUnitMultiplier();
  }
}
