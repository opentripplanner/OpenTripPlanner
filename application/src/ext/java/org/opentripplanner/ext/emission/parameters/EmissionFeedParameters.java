package org.opentripplanner.ext.emission.parameters;

import java.net.URI;
import org.opentripplanner.graph_builder.model.DataSourceConfig;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public record EmissionFeedParameters(String feedId, URI source) implements DataSourceConfig {
  @Override
  public String toString() {
    return ToStringBuilder.of(EmissionFeedParameters.class)
      .addStr("feedId", feedId)
      .addObj("source", source)
      .toString();
  }
}
