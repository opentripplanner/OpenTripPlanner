package org.opentripplanner.ext.transmodelapi._support;

import java.util.Map;
import java.util.function.Consumer;
import org.opentripplanner.ext.transmodelapi.support.DataFetcherDecorator;

public class TestDataFetcherDecorator extends DataFetcherDecorator {

  private final Map<String, ?> data;

  public TestDataFetcherDecorator(Map<String, ?> data) {
    super(null);
    this.data = data;
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
