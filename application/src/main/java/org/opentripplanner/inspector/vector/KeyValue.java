package org.opentripplanner.inspector.vector;

import java.util.Collection;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * A key value pair that represents data being sent to the vector tile library for visualisation
 * in a map (including popups).
 * <p>
 * The underlying format (and library) supports only a limited number of Java types and silently
 * drops those that aren't supported: https://github.com/CI-CMG/mapbox-vector-tile/blob/master/src/main/java/edu/colorado/cires/cmg/mvt/encoding/MvtValue.java#L18-L40
 * <p>
 * For this reason this class also has a static initializer that automatically converts common
 * OTP classes into vector tile-compatible strings.
 */
public record KeyValue(String key, Object value) {
  /**
   * Takes a key and value and builds an object for the vector tile serializer.
   * <p>
   * Special handling exists for {@link FeedScopedId} and {@link Enum} which are converted to
   * strings.
   */
  public static KeyValue kv(String key, @Nullable Object value) {
    if (value == null) {
      return new KeyValue(key, null);
    } else if (value instanceof FeedScopedId || value instanceof Enum<?>) {
      return new KeyValue(key, value.toString());
    } else {
      return new KeyValue(key, value);
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
