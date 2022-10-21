package org.opentripplanner.ext.datastore.gs;

import static java.nio.channels.Channels.newOutputStream;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import java.io.OutputStream;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.file.DirectoryDataSource;
import org.opentripplanner.datastore.file.ZipFileDataSource;

class GsOutFileDataSource extends AbstractGsDataSource implements DataSource {

  private final Storage storage;

  /**
   * Create a data source wrapper around a file. This wrapper handles GZIP(.gz) compressed files as
   * well as normal files. It does not handle directories({@link DirectoryDataSource}) or zip-files
   * {@link ZipFileDataSource} which contain multiple files.
   */
  GsOutFileDataSource(Storage storage, BlobId blobId, FileType type) {
    super(blobId, type);
    this.storage = storage;
  }

  @Override
  public boolean exists() {
    return false;
  }

  @Override
  public OutputStream asOutputStream() {
    BlobInfo blobInfo = BlobInfo.newBuilder(blobId()).build();
    return newOutputStream(storage.writer(blobInfo, Storage.BlobWriteOption.doesNotExist()));
  }
}
