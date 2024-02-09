package org.opentripplanner.inspector.vector;

public record KeyValue(String key, Object value) {
  public static KeyValue kv(String key, Object value) {
    return new KeyValue(key, value);
  }
}
