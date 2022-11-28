package org.opentripplanner.datastore.https;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.file.DirectoryDataSource;
import org.opentripplanner.datastore.file.ZipFileDataSource;
import org.opentripplanner.framework.io.HttpUtils;

/**
 * This class is a wrapper around an HTTPS resource.
 * <p>
 * Reading compressed HTTPS resources is supported. The only format supported is gzip (extension
 * .gz).
 */
record HttpsFileDataSource(URI uri, FileType type, HttpsDataSourceMetadata httpsDataSourceMetadata)
  implements DataSource {
  private static final Duration HTTP_GET_REQUEST_TIMEOUT = Duration.ofSeconds(20);

  /**
   * Create a data source wrapper around an HTTPS resource. This wrapper handles GZIP(.gz)
   * compressed files as well as normal files. It does not handle
   * directories({@link DirectoryDataSource}) or zip-files {@link ZipFileDataSource} which contain
   * multiple files.
   */

  @Override
  public long size() {
    return httpsDataSourceMetadata.contentLength();
  }

  @Override
  public long lastModified() {
    return httpsDataSourceMetadata.lastModified();
  }

  @Override
  public boolean exists() {
    return true;
  }

  @Override
  public boolean isWritable() {
    return false;
  }

  @Override
  public InputStream asInputStream() {
    InputStream in;

    try {
      in = HttpUtils.getData(uri, HTTP_GET_REQUEST_TIMEOUT, Map.of());
    } catch (IOException e) {
      throw new IllegalStateException(e.getLocalizedMessage(), e);
    }

    // We support both gzip and unzipped files when reading.

    if (httpsDataSourceMetadata().isGzipContentType() || uri.getPath().endsWith(".gz")) {
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
    throw new UnsupportedOperationException(
      "Write operations are not available for HTTPS data sources"
    );
  }

  @Override
  public String name() {
    return uri.getPath().substring(uri.getPath().lastIndexOf('/') + 1);
  }

  @Override
  public String path() {
    return uri.toString();
  }

  @Override
  public String directory() {
    int endIndex = path().lastIndexOf(name()) - 1;
    return endIndex <= 0 ? "" : path().substring(0, endIndex);
  }
}
