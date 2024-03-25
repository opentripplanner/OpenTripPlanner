package org.opentripplanner.transit.model.filter.expr;

import static org.opentripplanner.transit.model.filter.expr.BinaryOperator.AND;

import java.util.List;

/**
 * Takes a list of matchers and provides a single interface. All matchers in the list must match for
 * the composite matcher to return a match.
 *
 * @param <T> The entity type the AndMatcher matches.
 */
public final class AndMatcher<T> implements Matcher<T> {

  private final Matcher<T>[] matchers;

  private AndMatcher(List<Matcher<T>> matchers) {
    this.matchers = matchers.toArray(Matcher[]::new);
  }

  public static <T> Matcher<T> of(List<Matcher<T>> matchers) {
    // simplify a list of one element
    if (matchers.size() == 1) {
      return matchers.get(0);
    }
    return new AndMatcher<>(matchers);
  }

  @Override
  public boolean match(T entity) {
    for (var m : matchers) {
      if (!m.match(entity)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toString() {
    return "(" + AND.arrayToString(matchers) + ')';
  }
}
