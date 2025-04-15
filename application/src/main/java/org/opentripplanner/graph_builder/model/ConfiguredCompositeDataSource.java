package org.opentripplanner.graph_builder.model;

import org.opentripplanner.datastore.api.CompositeDataSource;

/**
 * A pair linking together a data source and its custom configuration.
 */
public class ConfiguredCompositeDataSource<T extends DataSourceConfig>
  extends ConfiguredDataSource<T> {

  public ConfiguredCompositeDataSource(CompositeDataSource dataSource, T config) {
    super(dataSource, config);
  }

  @Override
  public CompositeDataSource dataSource() {
    return (CompositeDataSource) super.dataSource();
  }
}
