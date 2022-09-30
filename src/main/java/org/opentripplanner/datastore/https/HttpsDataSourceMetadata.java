package org.opentripplanner.datastore.https;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.client.utils.DateUtils;

/**
 * HTTPS data source metadata returned by the HTTP server (HTTP headers).
 */
public class HttpsDataSourceMetadata {

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

  public HttpsDataSourceMetadata(List<Header> headers) {
    this(
      headers
        .stream()
        .filter(header -> HTTP_HEADERS.contains(header.getName()))
        .collect(Collectors.toUnmodifiableMap(Header::getName, Header::getValue))
    );
  }

  public HttpsDataSourceMetadata(Map<String, String> headers) {
    contentType = headers.get(HttpHeaders.CONTENT_TYPE);
    contentLength = parseLong(headers.get(HttpHeaders.CONTENT_LENGTH));
    lastModified = parseDate(headers.get(HttpHeaders.LAST_MODIFIED));
  }

  public String contentType() {
    return contentType;
  }

  public long contentLength() {
    return contentLength;
  }

  public long lastModified() {
    return lastModified;
  }

  public boolean isZipContentType() {
    return CONTENT_TYPE_APPLICATION_ZIP.equalsIgnoreCase(contentType());
  }

  public boolean isGzipContentType() {
    return CONTENT_TYPE_APPLICATION_GZIP.equalsIgnoreCase(contentType());
  }

  private static long parseDate(String lastModifiedHeader) {
    if (lastModifiedHeader != null) {
      Date lastModifiedDate = DateUtils.parseDate(lastModifiedHeader);
      if (lastModifiedDate != null) {
        return lastModifiedDate.getTime();
      }
    }
    return 0;
  }

  private static long parseLong(String header) {
    try {
      return Long.parseLong(header);
    } catch (Exception e) {
      return 0;
    }
  }
}
