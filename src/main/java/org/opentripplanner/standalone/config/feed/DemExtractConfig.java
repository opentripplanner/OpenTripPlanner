package org.opentripplanner.standalone.config.feed;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/**
 * Configuration of a DEM extract. Example:
 * {@code "dem" : [ {source: "file:///path/to/otp/norway-dem.tif"} ] }
 */
public class DemExtractConfig implements DataSourceConfig {

  private final URI source;

  private final Double elevationUnitMultiplier;

  DemExtractConfig(DemExtractConfigBuilder demExtractConfigBuilder) {
    source = Objects.requireNonNull(demExtractConfigBuilder.getSource());
    elevationUnitMultiplier = demExtractConfigBuilder.getElevationUnitMultiplier();
  }

  @Override
  public URI source() {
    return source;
  }

  /**
   * Returns the custom elevation unit multiplier for this feed. Overrides {@link org.opentripplanner.standalone.config.BuildConfig#elevationUnitMultiplier}
   */
  public Optional<Double> elevationUnitMultiplier() {
    return Optional.ofNullable(elevationUnitMultiplier);
  }
}
