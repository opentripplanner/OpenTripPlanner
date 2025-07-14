package org.opentripplanner.apis.support;

import io.micrometer.core.instrument.Tag;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.Collection;

public final class TracingUtils {

  private static final String UNKNOWN_VALUE = "__UNKNOWN__";

  /**
   * This method tries to find a tracing tag from a request's headers.
   * If no value is found for a tag, the value is set to "__UNKNOWN__".
   *
   * @param tracingHeaderTags a collection of tag names to match against headers
   * @param headers headers from a request
   * @return a list of tracing tags with computed values
   */
  public static Iterable<Tag> findTagsInHeaders(
    Collection<String> tracingHeaderTags,
    HttpHeaders headers
  ) {
    return tracingHeaderTags
      .stream()
      .map(header -> {
        String value = headers.getHeaderString(header);
        return Tag.of(header, value == null ? UNKNOWN_VALUE : value);
      })
      .toList();
  }

  /**
   * This method tries to find a tracing tag from either a request's headers or query parameters.
   * The value from headers is favored if a value is present in both.
   * If no value is found for a tag, the value is set to "__UNKNOWN__".
   *
   * @param tracingTags a collection of tag names to match against headers or query parameters
   * @param headers headers from a request
   * @param queryParameters query parameters from a request
   * @return a list of tracing tags with computed values
   */
  public static Iterable<Tag> findTagsInHeadersOrQueryParameters(
    Collection<String> tracingTags,
    HttpHeaders headers,
    MultivaluedMap<String, String> queryParameters
  ) {
    return tracingTags
      .stream()
      .map(header -> {
        String headerValue = headers.getHeaderString(header);
        String queryParameterValue = queryParameters.getFirst(header);
        if (headerValue != null) {
          return Tag.of(header, headerValue);
        } else if (queryParameterValue != null) {
          return Tag.of(header, queryParameterValue);
        } else {
          return Tag.of(header, UNKNOWN_VALUE);
        }
      })
      .toList();
  }
}
