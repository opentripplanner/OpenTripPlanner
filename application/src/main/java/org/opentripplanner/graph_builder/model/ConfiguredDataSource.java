package org.opentripplanner.graph_builder.model;

import java.util.Objects;
import org.opentripplanner.datastore.api.DataSource;

/**
 * A pair linking together a data source and its custom configuration.
 */
public class ConfiguredDataSource<T extends DataSourceConfig> {

  private DataSource dataSource;
  private T config;

  public ConfiguredDataSource(DataSource dataSource, T config) {
    this.dataSource = Objects.requireNonNull(dataSource, "'dataSource' is required");
    this.config = Objects.requireNonNull(config, "'config' is required");
    this.dataSource = dataSource;
  }

  public DataSource dataSource() {
    return dataSource;
  }

  public T config() {
    return config;
  }
}
