package org.opentripplanner.datastore.api;

import java.io.Closeable;
import java.util.Collection;

/**
 * A composite data source contain a collection of other {@link DataSource}s.
 * <p>
 * Example are file directories and zip files with gtfs or netex data.
 */
public interface CompositeDataSource extends DataSource, Closeable {
  /**
   * Open the composite data source and read the content. For a random access data source
   * (local-file system), this does not read each entry, but just the metadata for each of them.
   * But, for a streamed data source(cloud storage) it will fetch the entire content - this might be
   * using a lot of memory.
   */
  Collection<DataSource> content();

  /**
   * Retrieve a single entry by name, or {@code null} if not found.
   * <p>
   * Example:
   * <p>
   * {@code DataSource routesSrc = gtfsSource.entry("routes.txt")}
   */
  DataSource entry(String name);

  /**
   * Delete content and container in store.
   */
  default void delete() {
    throw new UnsupportedOperationException(
      "This datasource type " +
      getClass().getSimpleName() +
      " do not support DELETE. Can not delete: " +
      path()
    );
  }
}
