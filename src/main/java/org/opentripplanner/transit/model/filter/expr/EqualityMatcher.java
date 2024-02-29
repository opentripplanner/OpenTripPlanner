package org.opentripplanner.transit.model.filter.expr;

import java.util.function.Function;

/**
 * A matcher that checks if a value is equal to another value.
 * <p>
 * The value is provided by a function that takes the entity being matched as an argument.
 */
public class EqualityMatcher<T, V> implements Matcher<T> {

  private final String typeName;
  private final V value;
  private final Function<T, V> valueProvider;

  public EqualityMatcher(String typeName, V value, Function<T, V> valueProvider) {
    this.typeName = typeName;
    this.value = value;
    this.valueProvider = valueProvider;
  }

  @Override
  public boolean match(T entity) {
    return value.equals(valueProvider.apply(entity));
  }

  @Override
  public String toString() {
    return typeName + "==" + value;
  }
}
