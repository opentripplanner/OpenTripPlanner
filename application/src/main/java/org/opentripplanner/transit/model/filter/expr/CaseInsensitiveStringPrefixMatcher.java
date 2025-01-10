package org.opentripplanner.transit.model.filter.expr;

import java.util.function.Function;

/**
 * A matcher that checks if a string field starts with a given value.
 * <p/>
 * @param <T> The type of the entity being matched.
 */
public class CaseInsensitiveStringPrefixMatcher<T> implements Matcher<T> {

  private final String typeName;
  private final String value;
  private final Function<T, String> valueProvider;

  /**
   * @param typeName The typeName appears in the toString for easier debugging.
   * @param value - The String that may be a prefix.
   * @param valueProvider - A function that maps the entity being matched to the String being
   *                      checked for a prefix match.
   */
  public CaseInsensitiveStringPrefixMatcher(
    String typeName,
    String value,
    Function<T, String> valueProvider
  ) {
    this.typeName = typeName;
    this.value = value;
    this.valueProvider = valueProvider;
  }

  @Override
  public boolean match(T entity) {
    return valueProvider.apply(entity).toLowerCase().startsWith(value.toLowerCase());
  }

  @Override
  public String toString() {
    return typeName + " has prefix: " + value;
  }
}
