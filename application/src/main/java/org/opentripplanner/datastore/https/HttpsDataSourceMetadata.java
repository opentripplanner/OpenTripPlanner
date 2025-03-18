package org.opentripplanner.datastore.https;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.hc.client5.http.utils.DateUtils;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * HTTPS data source metadata returned by the HTTP server (HTTP headers).
 */
class HttpsDataSourceMetadata {

  static final String CONTENT_TYPE_APPLICATION_GZIP = "application/gzip";
  static final String CONTENT_TYPE_APPLICATION_ZIP = "application/zip";

  private static final Set<String> HTTP_HEADERS = Set.of(
    HttpHeaders.CONTENT_TYPE,
    HttpHeaders.CONTENT_LENGTH,
    HttpHeaders.LAST_MODIFIED
  );

  private final String contentType;
  private final long contentLength;
  private final long lastModified;

  HttpsDataSourceMetadata(List<Header> headers) {
    this(
      headers
        .stream()
        .filter(header -> HTTP_HEADERS.contains(header.getName()))
        .collect(Collectors.toUnmodifiableMap(Header::getName, Header::getValue))
    );
  }

  HttpsDataSourceMetadata(Map<String, String> headers) {
    contentType = headers.get(HttpHeaders.CONTENT_TYPE);
    contentLength = parseLong(headers.get(HttpHeaders.CONTENT_LENGTH));
    lastModified = parseDate(headers.get(HttpHeaders.LAST_MODIFIED));
  }

  String contentType() {
    return contentType;
  }

  long contentLength() {
    return contentLength;
  }

  long lastModified() {
    return lastModified;
  }

  boolean isZipContentType() {
    return CONTENT_TYPE_APPLICATION_ZIP.equalsIgnoreCase(contentType());
  }

  boolean isGzipContentType() {
    return CONTENT_TYPE_APPLICATION_GZIP.equalsIgnoreCase(contentType());
  }

  private static long parseDate(String lastModifiedHeader) {
    if (lastModifiedHeader != null) {
      Instant lastModifiedDate = DateUtils.parseStandardDate(lastModifiedHeader);
      if (lastModifiedDate != null) {
        return lastModifiedDate.toEpochMilli();
      }
    }
    return -1;
  }

  private static long parseLong(String header) {
    try {
      return Long.parseLong(header);
    } catch (Exception e) {
      return DataSource.UNKNOWN;
    }
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(this.getClass())
      .addObj("contentType", contentType)
      .addObj("contentLength", contentLength)
      .addObj("lastModified", lastModified)
      .toString();
  }
}
