package org.opentripplanner.datastore.https;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.apache.hc.core5.http.Header;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.base.DataSourceRepository;
import org.opentripplanner.datastore.file.ZipStreamDataSourceDecorator;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This data store accesses files in read-only mode over HTTPS.
 */
public class HttpsDataSourceRepository implements DataSourceRepository {

  private static final Logger LOG = LoggerFactory.getLogger(HttpsFileDataSource.class);

  private static final Duration HTTP_HEAD_REQUEST_TIMEOUT = Duration.ofSeconds(20);

  private final Set<URI> insecureSources;
  private final Set<URI> uncheckedZipSources;

  public HttpsDataSourceRepository() {
    this(Set.of(), Set.of());
  }

  public HttpsDataSourceRepository(Set<URI> insecureSources, Set<URI> uncheckedZipSources) {
    this.insecureSources = insecureSources;
    this.uncheckedZipSources = uncheckedZipSources;
  }

  @Override
  public String description() {
    return "HTTPS";
  }

  @Override
  public void open() {}

  @Override
  public DataSource findSource(URI uri, FileType type) {
    if (skipUri(uri)) {
      return null;
    }
    return createSource(uri, type);
  }

  @Override
  public CompositeDataSource findCompositeSource(URI uri, FileType type) {
    if (skipUri(uri)) {
      return null;
    }
    return createCompositeSource(uri, type);
  }

  /* private methods */

  private boolean skipUri(URI uri) {
    String scheme = uri.getScheme();
    if ("https".equals(scheme)) {
      return false;
    }

    return !("http".equals(scheme) && insecureSources.contains(uri));
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

    if (
      uncheckedZipSources.contains(uri) ||
      httpsDataSourceMetadata.isZipContentType() ||
      uri.getPath().endsWith(".zip")
    ) {
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
    try (OtpHttpClientFactory otpHttpClientFactory = new OtpHttpClientFactory()) {
      var otpHttpClient = otpHttpClientFactory.create(LOG);
      return otpHttpClient.getHeaders(uri, HTTP_HEAD_REQUEST_TIMEOUT, HttpHeaders.empty());
    }
  }
}
