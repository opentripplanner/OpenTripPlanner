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

  public static Builder of(String source) {
    return new Builder(source);
  }

  public static HttpHeaders of(Map<String, String> map, String source) {
    if (map.isEmpty()) {
      return empty();
    }
    var builder = of(source);
    for (Map.Entry<String, String> e : map.entrySet()) {
      builder.add(e.getKey(), e.getValue());
    }
    return builder.build();
  }

  public static HttpHeaders empty() {
    return of("<EMPTY>").build();
  }

  public Map<String, String> headers() {
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

    private final String source;

    private final HashMap<String, String> headers = new HashMap<>();

    public Builder(String source) {
      this.source = source;
    }

    public Builder acceptApplicationXML() {
      headers.put("Accept", "application/xml");
      return this;
    }

    public Builder add(String name, String value) {
      headers.put(name, tokenSubstitution(value));
      return this;
    }

    public HttpHeaders build() {
      return new HttpHeaders(this);
    }

    private String tokenSubstitution(String value) {
      return EnvironmentVariableReplacer.insertEnvironmentVariables(value, source);
    }
  }
}
