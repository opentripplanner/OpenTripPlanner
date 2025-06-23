package org.opentripplanner.apis;

import io.micrometer.core.instrument.Tag;
import jakarta.ws.rs.core.HttpHeaders;
import java.util.Collection;
import java.util.stream.Collectors;

public final class APIUtils {

  /**
   * TODO
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
}
