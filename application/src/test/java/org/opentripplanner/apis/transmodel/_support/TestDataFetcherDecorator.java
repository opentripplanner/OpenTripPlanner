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

  public static TestDataFetcherDecorator of(Map<String, ?> data) {
    return new TestDataFetcherDecorator(data);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> void argument(String name, Consumer<T> consumer) {
    call((Map<String, T>) data, name, consumer);
  }
}
