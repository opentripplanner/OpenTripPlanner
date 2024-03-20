package org.opentripplanner.transit.model.filter.expr;

import java.util.Collection;
import java.util.function.Function;

/**
 * A matcher that checks if a collection contains a value.
 * <p>
 * The collection is provided by a function that takes the entity being matched as an argument.
 */
public class ContainsMatcher<T, S> implements Matcher<T> {

  private final String typeName;
  private final S value;
  private final Function<T, Collection<S>> valueProvider;

  public ContainsMatcher(String typeName, S value, Function<T, Collection<S>> valueProvider) {
    this.typeName = typeName;
    this.value = value;
    this.valueProvider = valueProvider;
  }

  @Override
  public boolean match(T entity) {
    Collection<S> values = valueProvider.apply(entity);
    if (values == null) {
      return false;
    }
    return values.contains(value);
  }

  @Override
  public String toString() {
    return typeName + " contains " + value;
  }
}
