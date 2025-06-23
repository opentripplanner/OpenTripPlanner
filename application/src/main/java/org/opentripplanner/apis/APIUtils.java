package org.opentripplanner.apis;

import io.micrometer.core.instrument.Tag;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.Collection;
import java.util.stream.Collectors;

public final class APIUtils {

  /**
   * TODO
   *
   * @param tracingHeaderTags TODO
   * @param headers TODO
   * @return TODO
   */
  public static Iterable<Tag> getTagsFromHeaders(
    Collection<String> tracingHeaderTags,
    HttpHeaders headers
  ) {
    return tracingHeaderTags
      .stream()
      .map(header -> {
        String value = headers.getHeaderString(header);
        return Tag.of(header, value == null ? "__UNKNOWN__" : value);
      })
      .collect(Collectors.toList());
  }

  /**
   * This method tries to find a tag from either headers or query parameters.
   * The value from headers is favored if a value is present in both.
   *
   * @param tracingHeaderTags TODO
   * @param queryParameters TODO
   * @return TODO
   */
  public static Iterable<Tag> getTagsFromHeadersOrQueryParameters(
    Collection<String> tracingHeaderTags,
    HttpHeaders headers,
    MultivaluedMap<String, String> queryParameters
  ) {
    return tracingHeaderTags
      .stream()
      .map(header -> {
        String headerValue = headers.getHeaderString(header);
        String queryParameterValue = queryParameters.getFirst(header);
        if (headerValue != null) {
          return Tag.of(header, headerValue);
        } else if (queryParameterValue != null) {
          return Tag.of(header, queryParameterValue);
        } else {
          return Tag.of(header, "__UNKNOWN__");
        }
      })
      .collect(Collectors.toList());
  }
}
