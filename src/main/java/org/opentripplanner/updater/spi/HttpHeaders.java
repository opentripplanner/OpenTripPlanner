package org.opentripplanner.updater.spi;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.opentripplanner.standalone.config.framework.project.EnvironmentVariableReplacer;

/**
 * Encapsulate HTTP header parameters for use in updaters. The builder
 * will use the {@link EnvironmentVariableReplacer} to substitute TOKENS in the
 * values passed in. There is also a convenience method for adding
 * {@link Builder#acceptApplicationXML()}.
 */
public class HttpHeaders {

  private final Map<String, String> headers;

  private HttpHeaders(Builder builder) {
    this.headers = Map.copyOf(builder.headers);
  }

  public static Builder of() {
    return new Builder();
  }

  public static HttpHeaders of(Map<String, String> map) {
    if (map.isEmpty()) {
      return empty();
    }
    var builder = of();
    for (Map.Entry<String, String> e : map.entrySet()) {
      builder.add(e.getKey(), e.getValue());
    }
    return builder.build();
  }

  public static HttpHeaders empty() {
    return of().build();
  }

  public Map<String, String> asMap() {
    return headers;
  }

  @Override
  public String toString() {
    return HttpHeaders.class.getSimpleName() + headers;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    HttpHeaders that = (HttpHeaders) o;
    return Objects.equals(headers, that.headers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(headers);
  }

  public static class Builder {

    private final HashMap<String, String> headers = new HashMap<>();

    public Builder acceptApplicationXML() {
      headers.put("Accept", "application/xml");
      return this;
    }

    public Builder acceptProtobuf() {
      headers.put(
        "Accept",
        "application/x-google-protobuf, application/x-protobuf, application/protobuf, application/octet-stream, */*"
      );
      return this;
    }

    public Builder add(String name, String value) {
      headers.put(name, value);
      return this;
    }

    /**
     * Merge another instance of {@link HttpHeaders} into this builder.
     * <p>
     * NOTE: if there are headers with the same name then the added ones override the
     * already set ones!
     */
    public Builder add(HttpHeaders other) {
      headers.putAll(other.asMap());
      return this;
    }

    public HttpHeaders build() {
      return new HttpHeaders(this);
    }
  }
}
