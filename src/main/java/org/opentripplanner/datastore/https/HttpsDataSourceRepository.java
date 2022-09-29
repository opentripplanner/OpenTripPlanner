package org.opentripplanner.datastore.https;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.base.DataSourceRepository;
import org.opentripplanner.datastore.base.ZipStreamDataSourceDecorator;
import org.opentripplanner.util.HttpUtils;

/**
 * This data store accesses files in read-only mode over HTTPS.
 */
public class HttpsDataSourceRepository implements DataSourceRepository {

  private static final String CONTENT_TYPE_APPLICATION_ZIP = "application/zip";

  private static final Set<String> HTTP_HEADERS = Set.of(
    HttpHeaders.CONTENT_ENCODING,
    HttpHeaders.CONTENT_TYPE,
    HttpHeaders.LAST_MODIFIED
  );

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
    HttpsDataSourceMetadata httpsDataSourceMetadata = getHttpsDataSourceMetadata(uri);
    return new HttpsFileDataSource(uri, type, httpsDataSourceMetadata);
  }

  private CompositeDataSource createCompositeSource(URI uri, FileType type) {
    HttpsDataSourceMetadata httpsDataSourceMetadata = getHttpsDataSourceMetadata(uri);

    if (
      CONTENT_TYPE_APPLICATION_ZIP.equalsIgnoreCase(httpsDataSourceMetadata.contentType()) ||
      uri.getPath().endsWith(".zip")
    ) {
      DataSource httpsSource = new HttpsFileDataSource(uri, type, httpsDataSourceMetadata);
      return new ZipStreamDataSourceDecorator(httpsSource);
    } else {
      throw new UnsupportedOperationException(
        "Only ZIP archives are supported as composite sources for the HTTPS data source"
      );
    }
  }

  private static HttpsDataSourceMetadata getHttpsDataSourceMetadata(URI uri) {
    List<Header> headers = HttpUtils.getHeaders(uri);
    return new HttpsDataSourceMetadata(
      headers
        .stream()
        .filter(header -> HTTP_HEADERS.contains(header.getName()))
        .collect(Collectors.toUnmodifiableMap(Header::getName, Header::getValue))
    );
  }
}
