package org.opentripplanner.ext.empiricaldelay.parameters;

import java.io.Serializable;
import java.net.URI;
import org.opentripplanner.graph_builder.model.DataSourceConfig;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Configuration parameters for a single empirical delay feed data source.
 */
public record EmpiricalDelayFeedParameters(String feedId, URI source)
  implements DataSourceConfig, Serializable {
  @Override
  public String toString() {
    return ToStringBuilder.of(EmpiricalDelayFeedParameters.class)
      .addStr("feedId", feedId)
      .addObj("source", source)
      .toString();
  }
}
