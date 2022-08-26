package org.opentripplanner.standalone.config.feed;

import java.net.URI;
import java.util.Objects;

/**
 * Configuration of a DEM extract. Example:
 * {@code "dem" : [ {source: "file:///path/to/otp/norway-dem.tif"} ] }
 */
public class DemExtractConfig implements DataSourceConfig {

  private final URI source;

  private final double elevationUnitMultiplier;

  DemExtractConfig(DemExtractConfigBuilder demExtractConfigBuilder) {
    source = Objects.requireNonNull(demExtractConfigBuilder.getSource());
    elevationUnitMultiplier = demExtractConfigBuilder.getElevationUnitMultiplier();
  }

  @Override
  public URI source() {
    return source;
  }

  public double elevationUnitMultiplier() {
    return elevationUnitMultiplier;
  }
}
