package org.opentripplanner.inspector.vector;

import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.stream.Collectors;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * A key value pair that represents data being sent to the vector tile library for visualisation
 * in a map (including popups).
 * <p>
 * The underlying format (and library) supports only a limited number of Java types and silently
 * drops those that aren't supported: https://github.com/CI-CMG/mapbox-vector-tile/blob/master/src/main/java/edu/colorado/cires/cmg/mvt/encoding/MvtValue.java#L18-L40
 * <p>
 * For this reason this class also has static initializer that automatically converts common
 * OTP classes into vector tile-compatible strings.
 */
public record KeyValue(String key, Object value) {
  public static KeyValue kv(String key, Object value) {
    return new KeyValue(key, value);
  }

  /**
   * A {@link FeedScopedId} is not a type that can be converted to a vector tile feature property
   * value. Therefore, we convert it to a string after performing a null check.
   */
  public static KeyValue kv(String key, @Nullable FeedScopedId value) {
    if (value != null) {
      return new KeyValue(key, value.toString());
    } else {
      return new KeyValue(key, null);
    }
  }

  /**
   * Takes a key and a collection of values, calls toString on the values and joins them using
   * comma as the separator.
   */
  public static KeyValue kColl(String key, Collection<?> value) {
    var values = value.stream().map(Object::toString).collect(Collectors.joining(","));
    return new KeyValue(key, values);
  }
}
