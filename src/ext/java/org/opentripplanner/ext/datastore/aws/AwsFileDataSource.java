package org.opentripplanner.ext.datastore.aws;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.file.DirectoryDataSource;
import org.opentripplanner.datastore.file.ZipFileDataSource;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * This class is a wrapper around and EXISTING Google Cloud Store bucket blob. It can be read and
 * overwritten.
 * <p>
 * Reading compressed blobs is supported. The only format supported is gzip (extension .gz).
 */
class AwsFileDataSource extends AbstractAwsDataSource implements DataSource {

  /**
   * Create a data source wrapper around a file. This wrapper handles GZIP(.gz) compressed files as
   * well as normal files. It does not handle directories({@link DirectoryDataSource}) or zip-files
   * {@link ZipFileDataSource} which contain multiple files.
   */
  AwsFileDataSource(S3Client s3Client, S3Object object, FileType type) {
    super(s3Client, object, type);
  }

  @Override
  public long size() {
    /*TODO*/
    return super.size();
  }

  @Override
  public long lastModified() {
    /*TODO*/
    return super.lastModified();
  }

  @Override
  public boolean exists() {
    /*TODO*/
    return super.exists();
  }

  @Override
  public boolean isWritable() {
    return true;
  }

  @Override
  public InputStream asInputStream() {
    InputStream in = null;
    /* TODO */

    // We support both gzip and unzipped files when reading.
    if (object().name().endsWith(".gz")) {
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
    /* TODO */
    return null;
  }
}
