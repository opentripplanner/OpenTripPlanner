package org.opentripplanner.datastore.base;

import java.net.URI;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;

/**
 * It is an abstraction to reading of and writing to files whenever the file is located on the local
 * disk or is in the cloud.
 * <p>
 * This interface provide an abstraction layer for accessing OTP data input and output sources. It
 * is an abstraction for reading from and/or writing to files whenever the file is located on the
 * local disk or is in the cloud. The interface make it possible to implement alternative ways to
 * access data. In a cloud ecosystem you might find it easier to access the data directly from the
 * cloud storage, rather than first copy the data into your node local disk, and then copy the build
 * graph back into cloud storage after building it. Depending on the source this might also offer
 * enhanced performance.
 * <p>
 * The {@link org.opentripplanner.datastore.OtpDataStore} will manage all repositories so there is
 * no need to use the repository directly.
 */
public interface DataSourceRepository {
  /**
   * @return a description that identify the datasource which is helpful to the user in case an
   * error occurs when using the repository.
   */
  String description();

  /**
   * Open a connection to the data repository/storage. This method is called once before any read
   * operations.
   */
  void open();

  /**
   * Get the a data source for the given uri and type.
   * <p>
   * The source may or may not {@link DataSource#exists()}.
   *
   * @param uri  a uniq URI to get the data source.
   * @param type the file type to load.
   * @return the datasource wrapper that can be used to access the data source.
   */
  DataSource findSource(URI uri, FileType type);

  /**
   * Get the a composite data source (zip/directory) for the given uri and type.
   * <p>
   * The source may or may not {@link DataSource#exists()}.
   *
   * @param uri  a uniq URI to get the data source.
   * @param type the file type to load.
   * @return the datasource wrapper that can be used to access the data source.
   */
  CompositeDataSource findCompositeSource(URI uri, FileType type);
}
