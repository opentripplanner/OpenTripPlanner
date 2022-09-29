package org.opentripplanner.datastore.https;

import java.util.Date;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.apache.http.client.utils.DateUtils;

/**
 * HTTPS data source metadata returned by the HTTP server (HTTP headers).
 */
public class HttpsDataSourceMetadata {
  private final String contentEncoding;
  private final String contentType;
  private final long lastModified;

  public HttpsDataSourceMetadata(Map<String, String> headers) {
    contentEncoding = headers.get(HttpHeaders.CONTENT_ENCODING);
    contentType = headers.get(HttpHeaders.CONTENT_TYPE);
    lastModified = parseDate(headers.get(HttpHeaders.LAST_MODIFIED));
  }

  public String contentEncoding() {
    return contentEncoding;
  }

  public String contentType() {
    return contentType;
  }

  public long lastModified() {
    return lastModified;
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

}
