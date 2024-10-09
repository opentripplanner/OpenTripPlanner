package org.opentripplanner.graph_builder.model;

import java.net.URI;

/**
 * Specific configuration applied to a single {@link org.opentripplanner.datastore.api.DataSource}
 */
public interface DataSourceConfig {
  /**
   * URI to data files.
   * <p>
   * Example:
   * {@code "file:///Users/kelvin/otp/netex.zip", "gs://my-bucket/netex.zip"  }
   * <p>
   * @return the URI to the data source. The source identifies uniquely a data source.
   */
  URI source();
}
