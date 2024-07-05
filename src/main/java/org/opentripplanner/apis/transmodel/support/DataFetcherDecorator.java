package org.opentripplanner.apis.transmodel.support;

import graphql.schema.DataFetchingEnvironment;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;

public class DataFetcherDecorator {

  private final DataFetchingEnvironment environment;

  public DataFetcherDecorator(DataFetchingEnvironment e) {
    this.environment = e;
  }

  public static <T> boolean hasArgument(Map<String, T> m, String name) {
    return m.containsKey(name) && m.get(name) != null;
  }

  public <T> void argument(String name, Consumer<T> consumer) {
    call(environment, name, consumer);
  }

  private static <T> void call(Map<String, T> m, String name, Consumer<T> consumer) {
    if (!name.contains(".")) {
      if (hasArgument(m, name)) {
        T v = m.get(name);
        consumer.accept(v);
      }
    } else {
      String[] parts = name.split("\\.");
      if (hasArgument(m, parts[0])) {
        Map<String, T> nm = (Map<String, T>) m.get(parts[0]);
        call(nm, String.join(".", Arrays.copyOfRange(parts, 1, parts.length)), consumer);
      }
    }
  }

  private static <T> void call(
    DataFetchingEnvironment environment,
    String name,
    Consumer<T> consumer
  ) {
    if (!name.contains(".")) {
      if (GqlUtil.hasArgument(environment, name)) {
        consumer.accept(environment.getArgument(name));
      }
    } else {
      String[] parts = name.split("\\.");
      if (GqlUtil.hasArgument(environment, parts[0])) {
        Map<String, T> nm = environment.getArgument(parts[0]);
        call(nm, String.join(".", Arrays.copyOfRange(parts, 1, parts.length)), consumer);
      }
    }
  }
}
