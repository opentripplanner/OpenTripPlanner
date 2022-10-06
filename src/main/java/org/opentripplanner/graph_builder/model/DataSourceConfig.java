package org.opentripplanner.graph_builder.model;

import java.net.URI;

/**
 * Specific configuration applied to a single {@link org.opentripplanner.datastore.api.DataSource}
 */
public interface DataSourceConfig {
  /**
   *
   * @return the URI to the data source. The source identifies uniquely a data source.
   */
  URI source();
}
