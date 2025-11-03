package org.opentripplanner.framework.io;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.hc.core5.http.Header;

/**
 * Represents an HTTP response containing the body content, headers and status code.
 * This class provides access to HTTP response headers and the response body as an InputStream.
 * <p>
 * The InputStream lifecycle is managed by OtpHttpClient. Callers must not close the stream.
 */
public class OtpHttpResponse {

  private final InputStream body;
  private final Map<String, List<String>> headers;
  private final int statusCode;

  /**
   * Creates an HTTP response wrapper.
   *
   * @param body the response body as an InputStream
   * @param rawHeaders the HTTP headers from the response
   * @param statusCode the HTTP status code from the response
   */
  OtpHttpResponse(InputStream body, Header[] rawHeaders, int statusCode) {
    this.body = Objects.requireNonNull(body, "body cannot be null");
    Objects.requireNonNull(rawHeaders, "rawHeaders cannot be null");
    this.headers = convertHeaders(rawHeaders);
    this.statusCode = statusCode;
  }

  /**
   * Returns the response body as an InputStream.
   * <p>
   * <strong>Important:</strong> The caller must NOT close this stream. Stream lifecycle is
   * managed by OtpHttpClient.
   *
   * @return the response body input stream
   */
  public InputStream body() {
    return body;
  }

  /**
   * Returns all response headers as an immutable map.
   * <p>
   * Header names are case-insensitive. The map keys are in lowercase.
   * Multiple header values with the same name are stored as a list.
   *
   * @return an immutable map of headers
   */
  public Map<String, List<String>> headers() {
    return headers;
  }

  /**
   * Returns the first value of the specified header, or empty if not present.
   * <p>
   * Header name comparison is case-insensitive.
   *
   * @param name the header name (case-insensitive)
   * @return the first header value, or empty if header is not present
   */
  public Optional<String> header(String name) {
    List<String> values = headers.get(normalizeName(name));
    if (values == null || values.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(values.get(0));
  }

  /**
   * Returns all values of the specified header.
   * <p>
   * Header name comparison is case-insensitive.
   * Returns an empty list if the header is not present.
   *
   * @param name the header name (case-insensitive)
   * @return a list of header values (empty if header not present)
   */
  public List<String> headerValues(String name) {
    List<String> values = headers.get(normalizeName(name));
    return values != null ? values : Collections.emptyList();
  }

  /**
   * Returns the status code of the http response
   */
  public int statusCode() {
    return statusCode;
  }

  /**
   * Converts Apache HttpComponents headers to a case-insensitive map structure.
   * <p>
   * All header names are normalized to lowercase for case-insensitive lookup.
   * Multiple headers with the same name are stored as a list of values.
   */
  private static Map<String, List<String>> convertHeaders(Header[] rawHeaders) {
    Map<String, List<String>> headerMap = new HashMap<>();

    for (Header header : rawHeaders) {
      String name = normalizeName(header.getName());
      String value = header.getValue();

      headerMap.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
    }

    // Make the map and all lists immutable
    Map<String, List<String>> immutableMap = new HashMap<>();
    for (Map.Entry<String, List<String>> entry : headerMap.entrySet()) {
      immutableMap.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
    }

    return Collections.unmodifiableMap(immutableMap);
  }

  /**
   * Normalizes a header name to lowercase for case-insensitive comparison.
   */
  private static String normalizeName(String name) {
    return name.toLowerCase(Locale.ROOT);
  }
}
