package org.opentripplanner.graph_builder.model;

import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;

/**
 *
 * A pair linking together a data source and its custom configuration.
 */
public class ConfiguredCompositeDataSource<T extends DataSourceConfig>
  extends ConfiguredDataSource<T> {

  public ConfiguredCompositeDataSource(DataSource dataSource, T config) {
    super(dataSource, config);
    if (!(dataSource instanceof CompositeDataSource)) {
      throw new IllegalArgumentException("Expected type CompositeDataSource for 'dataSource'.");
    }
  }

  @Override
  public CompositeDataSource dataSource() {
    return (CompositeDataSource) super.dataSource();
  }
}
