package org.opentripplanner.standalone.config.feed;

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
