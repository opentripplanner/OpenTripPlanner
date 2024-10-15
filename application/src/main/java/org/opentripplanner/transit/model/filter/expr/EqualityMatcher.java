package org.opentripplanner.transit.model.filter.expr;

import java.util.function.Function;

/**
 * A matcher that checks if a value is equal to another value derived from the matched entities.
 * <p/>
 * The derived entity value is provided by a function that takes the entity being matched as an argument.
 * <p/>
 * @param <T> The type of the entity being matched.
 * @param <V> The type of the value that the matcher will test equality for.
 */
public class EqualityMatcher<T, V> implements Matcher<T> {

  private final String typeName;
  private final V value;
  private final Function<T, V> valueProvider;

  /**
   * @param typeName The typeName appears in the toString for easier debugging.
   * @param value The value that this matcher will check equality for.
   * @param valueProvider The function that maps the entity being matched by this matcher (T) to
   *                      the value being matched by this matcher.
   */
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
