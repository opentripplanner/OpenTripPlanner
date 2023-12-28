package org.opentripplanner.apis.transmodel._support;

import java.util.Map;
import java.util.function.Consumer;
import org.opentripplanner.apis.transmodel.support.DataFetcherDecorator;

public class TestDataFetcherDecorator extends DataFetcherDecorator {

  private final Map<String, ?> data;

  private TestDataFetcherDecorator(Map<String, ?> data) {
    super(null);
    this.data = data;
  }

  public static TestDataFetcherDecorator of(String fieldName, Object value) {
    return new TestDataFetcherDecorator(Map.of(fieldName, value));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> void argument(String name, Consumer<T> consumer) {
    T value = (T) data.get(name);
    if (value != null) {
      consumer.accept(value);
    } else {
      System.out.println("No mapping for: " + name);
    }
  }
}
