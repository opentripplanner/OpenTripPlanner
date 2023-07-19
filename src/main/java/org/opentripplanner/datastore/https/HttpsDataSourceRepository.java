package org.opentripplanner.datastore.https;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.apache.hc.core5.http.Header;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.base.DataSourceRepository;
import org.opentripplanner.datastore.file.ZipStreamDataSourceDecorator;
import org.opentripplanner.framework.io.OtpHttpClient;

/**
 * This data store accesses files in read-only mode over HTTPS.
 */
public class HttpsDataSourceRepository implements DataSourceRepository {

  private static final Duration HTTP_HEAD_REQUEST_TIMEOUT = Duration.ofSeconds(20);

  @Override
  public String description() {
    return "HTTPS";
  }

  @Override
  public void open() {}

  @Override
  public DataSource findSource(@Nonnull URI uri, @Nonnull FileType type) {
    if (skipUri(uri)) {
      return null;
    }
    return createSource(uri, type);
  }

  @Override
  public CompositeDataSource findCompositeSource(@Nonnull URI uri, @Nonnull FileType type) {
    if (skipUri(uri)) {
      return null;
    }
    return createCompositeSource(uri, type);
  }

  /* private methods */

  private static boolean skipUri(URI uri) {
    return !"https".equals(uri.getScheme());
  }

  private DataSource createSource(URI uri, FileType type) {
    HttpsDataSourceMetadata httpsDataSourceMetadata = new HttpsDataSourceMetadata(
      getHttpHeaders(uri)
    );
    return new HttpsFileDataSource(uri, type, httpsDataSourceMetadata);
  }

  private CompositeDataSource createCompositeSource(URI uri, FileType type) {
    HttpsDataSourceMetadata httpsDataSourceMetadata = new HttpsDataSourceMetadata(
      getHttpHeaders(uri)
    );

    if (httpsDataSourceMetadata.isZipContentType() || uri.getPath().endsWith(".zip")) {
      DataSource httpsSource = new HttpsFileDataSource(uri, type, httpsDataSourceMetadata);
      return new ZipStreamDataSourceDecorator(httpsSource);
    } else {
      throw new UnsupportedOperationException(
        "Only ZIP archives are supported as composite sources for the HTTPS data source. URL: %s".formatted(
            uri
          )
      );
    }
  }

  protected List<Header> getHttpHeaders(URI uri) {
    try (OtpHttpClient otpHttpClient = new OtpHttpClient()) {
      return otpHttpClient.getHeaders(uri, HTTP_HEAD_REQUEST_TIMEOUT, Map.of());
    }
  }
}
