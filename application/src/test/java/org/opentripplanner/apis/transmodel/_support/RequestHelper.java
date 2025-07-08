package org.opentripplanner.apis.transmodel._support;

import java.util.List;
import java.util.Map;

public class RequestHelper {

  public static <T> List<T> list(T... values) {
    return List.of(values);
  }

  public static <T> Map<String, T> map(Map.Entry<String, ? extends T>... entries) {
    return Map.ofEntries(entries);
  }

  public static <T> Map<String, T> map(String key, T value) {
    return Map.of(key, value);
  }
}
