package org.opentripplanner.framework.io;

import java.util.Optional;
import org.apache.hc.core5.http.Header;

/**
 * Wrapper for HTTP response data including status code, headers, and parsed body.
 * Used to support conditional requests (ETag/If-None-Match) where we need access
 * to both response headers and the parsed body.
 *
 * @param <T> The type of the response body
 */
public class HttpResponseWithHeaders<T> {

  private final int statusCode;
  private final Header[] headers;
  private final T body;

  public HttpResponseWithHeaders(int statusCode, Header[] headers, T body) {
    this.statusCode = statusCode;
    this.headers = headers;
    this.body = body;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public Header[] getHeaders() {
    return headers;
  }

  public T getBody() {
    return body;
  }

  /**
   * Returns the value of the first header with the given name, if present.
   */
  public Optional<String> getHeader(String name) {
    if (headers == null) {
      return Optional.empty();
    }
    for (Header header : headers) {
      if (header.getName().equalsIgnoreCase(name)) {
        return Optional.ofNullable(header.getValue());
      }
    }
    return Optional.empty();
  }

  /**
   * Returns true if the response status code indicates success (2xx).
   */
  public boolean isSuccess() {
    return statusCode >= 200 && statusCode < 300;
  }

  /**
   * Returns true if the response is 304 Not Modified.
   */
  public boolean isNotModified() {
    return statusCode == 304;
  }
}
