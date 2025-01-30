package org.opentripplanner.transit.model.filter.expr;

import java.util.function.Function;

/**
 * A matcher that validates that a value is not null before applying another matcher. A useful case
 * is when you want to check that a String field is not null before applying a {@link CaseInsensitiveStringPrefixMatcher}.
 * <p/>
 * @param <T> The type of the entity being matched.
 * @param <V> The type of the value that the matcher will test for not null.
 */
public class NullSafeWrapperMatcher<T, V> implements Matcher<T> {

  private final String typeName;
  private final Function<T, V> valueProvider;
  private final Matcher<T> valueMatcher;

  /**
   * @param typeName The typeName appears in the toString for easier debugging.
   * @param valueProvider The function that maps the entity being matched by this matcher (T) to
   *                      the value being checked for non-null.
   */
  public NullSafeWrapperMatcher(
    String typeName,
    Function<T, V> valueProvider,
    Matcher<T> valueMatcher
  ) {
    this.typeName = typeName;
    this.valueProvider = valueProvider;
    this.valueMatcher = valueMatcher;
  }

  @Override
  public boolean match(T entity) {
    return valueProvider.apply(entity) != null && valueMatcher.match(entity);
  }

  @Override
  public String toString() {
    return typeName + " is not null";
  }
}
