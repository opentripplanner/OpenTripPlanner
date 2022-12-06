package org.opentripplanner.graph_builder.module.ned.parameter;

import java.net.URI;
import org.opentripplanner.graph_builder.model.DataSourceConfig;

/**
 * Configuration of a DEM extract. Example:
 * {@code "dem" : [ {source: "file:///path/to/otp/norway-dem.tif"} ] }
 */
public record DemExtractParameters(URI source, Double elevationUnitMultiplier)
  implements DataSourceConfig {
  public static final double DEFAULT_ELEVATION_UNIT_MULTIPLIER = 1;

  public static final DemExtractParameters DEFAULT = new DemExtractParametersBuilder().build();

  DemExtractParameters(DemExtractParametersBuilder builder) {
    this(builder.source(), builder.elevationUnitMultiplier());
  }

  @Override
  public URI source() {
    return source;
  }

  public DemExtractParametersBuilder copyOf() {
    return new DemExtractParametersBuilder(this);
  }
}
