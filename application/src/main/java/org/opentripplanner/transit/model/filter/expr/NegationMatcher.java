package org.opentripplanner.transit.model.filter.expr;

import java.util.function.Function;

/**
 * A matcher that validates that a value is not null before applying another matcher. A useful case
 * is when you want to check that a String field is not null before applying a {@link CaseInsensitiveStringPrefixMatcher}.
 * <p/>
 * @param <T> The type of the entity being matched.
 * @param <V> The type of the value that the matcher will test for not null.
 */
public class NegationMatcher<T, V> implements Matcher<T> {

  private final String typeName;
  private final Matcher<T> valueMatcher;

  /**
   * @param typeName The typeName appears in the toString for easier debugging.
   */
  public NegationMatcher(String typeName, Matcher<T> valueMatcher) {
    this.typeName = typeName;
    this.valueMatcher = valueMatcher;
  }

  @Override
  public boolean match(T entity) {
    return !valueMatcher.match(entity);
  }

  @Override
  public String toString() {
    return typeName + " is not null";
  }
}
