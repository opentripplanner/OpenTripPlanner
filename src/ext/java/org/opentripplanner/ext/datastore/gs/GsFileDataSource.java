package org.opentripplanner.ext.datastore.gs;

import static java.nio.channels.Channels.newInputStream;
import static java.nio.channels.Channels.newOutputStream;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.file.DirectoryDataSource;
import org.opentripplanner.datastore.file.ZipFileDataSource;

/**
 * This class is a wrapper around and EXISTING Google Cloud Store bucket blob. It can be read and
 * overwritten.
 * <p>
 * Reading compressed blobs is supported. The only format supported is gzip (extension .gz).
 */
class GsFileDataSource extends AbstractGsDataSource implements DataSource {

  private final Blob blob;

  /**
   * Create a data source wrapper around a file. This wrapper handles GZIP(.gz) compressed files as
   * well as normal files. It does not handle directories({@link DirectoryDataSource}) or zip-files
   * {@link ZipFileDataSource} which contain multiple files.
   */
  GsFileDataSource(Blob blob, FileType type) {
    super(blob.getBlobId(), type);
    this.blob = blob;
  }

  @Override
  public long size() {
    return blob.getSize();
  }

  @Override
  public long lastModified() {
    return blob.getUpdateTime();
  }

  @Override
  public boolean exists() {
    return blob.exists();
  }

  @Override
  public boolean isWritable() {
    return true;
  }

  @Override
  public InputStream asInputStream() {
    // We support both gzip and unzipped files when reading.
    InputStream in = newInputStream(blob.reader());

    if (blob.getName().endsWith(".gz")) {
      try {
        return new GZIPInputStream(in);
      } catch (IOException e) {
        throw new IllegalStateException(e.getLocalizedMessage(), e);
      }
    } else {
      return in;
    }
  }

  @Override
  public OutputStream asOutputStream() {
    return newOutputStream(blob.writer(Storage.BlobWriteOption.generationMatch()));
  }
}
