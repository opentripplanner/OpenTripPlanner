package org.opentripplanner.graph_builder;

import java.util.Objects;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.graph_builder.model.DataSourceConfig;

/**
 *
 * A pair linking together a data source and its custom configuration.
 */
public record ConfiguredDataSource<T extends DataSourceConfig>(DataSource dataSource, T config) {
  public ConfiguredDataSource {
    Objects.requireNonNull(dataSource, "'dataSource' is required");
    Objects.requireNonNull(config, "'config' is required");
  }
}
