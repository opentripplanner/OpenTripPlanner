package org.opentripplanner.graph_builder;

import java.util.Objects;
import org.opentripplanner.datastore.api.DataSource;

public record ConfiguredDataSource<T>(DataSource dataSource, T config) {
  public ConfiguredDataSource {
    Objects.requireNonNull(dataSource, "'dataSource' is required");
    Objects.requireNonNull(config, "'config' is required");
  }
}
