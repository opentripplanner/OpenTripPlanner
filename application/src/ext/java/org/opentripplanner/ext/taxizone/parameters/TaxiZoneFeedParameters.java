package org.opentripplanner.ext.taxizone.parameters;

import java.io.Serializable;
import java.net.URI;
import org.opentripplanner.graph_builder.model.DataSourceConfig;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Configuration parameters for a single taxi zone feed data source.
 */
public record TaxiZoneFeedParameters(
  String feedId,
  URI source
) implements DataSourceConfig, Serializable {
  @Override
  public String toString() {
    return ToStringBuilder.of(TaxiZoneFeedParameters.class)
      .addStr("feedId", feedId)
      .addObj("source", source)
      .toString();
  }
}
