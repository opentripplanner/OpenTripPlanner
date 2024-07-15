package org.opentripplanner.inspector.vector;

import java.util.Collection;
import java.util.stream.Collectors;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public record KeyValue(String key, Object value) {
  public static KeyValue kv(String key, Object value) {
    return new KeyValue(key, value);
  }
  public static KeyValue kv(String key, FeedScopedId value) {
    if(value !=null){
      return new KeyValue(key, value.toString());
    }
    else {
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
