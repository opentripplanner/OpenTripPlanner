package org.opentripplanner.transit.model.filter.expr;

/**
 * A matcher that validates that a value does NOT match the matcher passed.
 * <p/>
 * @param <T> The type of the entity being matched.
 */
public class NegationMatcher<T> implements Matcher<T> {

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
    return "is not" + typeName;
  }
}
