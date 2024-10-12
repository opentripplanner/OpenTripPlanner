package org.opentripplanner.datastore.base;

import java.util.List;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;

/**
 * This extends the {@link DataSourceRepository} and add the ability to retrieve files by name and
 * provide a list of auto discovered data sources, listing all sources (files) available by type.
 */
public interface LocalDataSourceRepository extends DataSourceRepository {
  /**
   * The directory is the same as the parent
   */
  String PARENT_DIRECTORY = ".";

  /**
   * Return true if the given filename is the same directory
   */
  static boolean isCurrentDir(String filename) {
    return PARENT_DIRECTORY.equals(filename);
  }

  /**
   * Get the a data source for the given localFilename and type.
   * <p>
   * The source may or may not {@link DataSource#exists()}.
   *
   * @param localFilename the short name including extension like: {@code graph.obj}.
   * @param type          the file type to load.
   * @return the datasource wrapper that can be used to access the data source.
   */
  DataSource findSource(String localFilename, FileType type);

  /**
   * Get the a composite data source (zip/directory) for the given uri and type.
   * <p>
   * The source may or may not {@link DataSource#exists()}.
   *
   * @param localFilename the short name including extension like: {@code gtfs.zip}.
   * @param type          the file type to load.
   * @return the datasource wrapper that can be used to access the data source.
   */
  CompositeDataSource findCompositeSource(String localFilename, FileType type);

  /**
   * List all existing data sources for the given type.
   *
   * @param type the file type to load.
   * @return the datasource wrapper that can be used to access the data source. Depending on the
   * type, the returned data source can be safely casted to a sub-type.
   */
  List<DataSource> listExistingSources(FileType type);
}
