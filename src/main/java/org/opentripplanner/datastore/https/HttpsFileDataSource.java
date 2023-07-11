package org.opentripplanner.datastore.https;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.file.DirectoryDataSource;
import org.opentripplanner.datastore.file.ZipFileDataSource;
import org.opentripplanner.framework.io.OtpHttpClient;

/**
 * This class is a wrapper around an HTTPS resource.
 * <p>
 * Reading compressed HTTPS resources is supported. The only format supported is gzip (extension
 * .gz).
 */
final class HttpsFileDataSource implements DataSource {

  private static final Duration HTTP_GET_REQUEST_TIMEOUT = Duration.ofSeconds(20);
  private final URI uri;
  private final FileType type;
  private final HttpsDataSourceMetadata httpsDataSourceMetadata;
  private final OtpHttpClient otpHttpClient;

  /**
   *
   */
  HttpsFileDataSource(URI uri, FileType type, HttpsDataSourceMetadata httpsDataSourceMetadata) {
    this.uri = uri;
    this.type = type;
    this.httpsDataSourceMetadata = httpsDataSourceMetadata;
    otpHttpClient = new OtpHttpClient();
  }

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
      in = otpHttpClient.getAsInputStream(uri, HTTP_GET_REQUEST_TIMEOUT, Map.of());
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

  @Override
  public URI uri() {
    return uri;
  }

  @Override
  public FileType type() {
    return type;
  }

  public HttpsDataSourceMetadata httpsDataSourceMetadata() {
    return httpsDataSourceMetadata;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (HttpsFileDataSource) obj;
    return (
      Objects.equals(this.uri, that.uri) &&
      Objects.equals(this.type, that.type) &&
      Objects.equals(this.httpsDataSourceMetadata, that.httpsDataSourceMetadata)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(uri, type, httpsDataSourceMetadata);
  }

  @Override
  public String toString() {
    return (
      "HttpsFileDataSource[" +
      "uri=" +
      uri +
      ", " +
      "type=" +
      type +
      ", " +
      "httpsDataSourceMetadata=" +
      httpsDataSourceMetadata +
      ']'
    );
  }
}
