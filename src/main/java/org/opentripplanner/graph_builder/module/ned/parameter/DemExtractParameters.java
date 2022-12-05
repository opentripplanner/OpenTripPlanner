package org.opentripplanner.graph_builder.module.ned.parameter;

import java.net.URI;
import org.opentripplanner.graph_builder.model.DataSourceConfig;

/**
 * Configuration of a DEM extract. Example:
 * {@code "dem" : [ {source: "file:///path/to/otp/norway-dem.tif"} ] }
 */
public class DemExtractParameters implements DataSourceConfig {

  public static final double DEFAULT_ELEVATION_UNIT_MULTIPLIER = 1;

  public static final DemExtractParameters DEFAULT = new DemExtractParametersBuilder().build();

  private final URI source;

  /**
   * Unit conversion multiplier for elevation values. No conversion needed if the elevation values
   * are defined in meters in the source data. If, for example, decimetres are used in the source
   * data, this should be set to 0.1.
   */
  private final Double elevationUnitMultiplier;

  DemExtractParameters(DemExtractParametersBuilder builder) {
    source = builder.source();
    elevationUnitMultiplier = builder.elevationUnitMultiplier();
  }

  @Override
  public URI source() {
    return source;
  }

  /**
   * Returns the custom elevation unit multiplier for this feed.
   */
  public Double elevationUnitMultiplier() {
    return elevationUnitMultiplier;
  }

  public DemExtractParametersBuilder copyOf() {
    return new DemExtractParametersBuilder(this);
  }
}
