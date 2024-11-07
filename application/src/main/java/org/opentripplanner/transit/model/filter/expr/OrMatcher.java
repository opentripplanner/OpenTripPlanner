package org.opentripplanner.transit.model.filter.expr;

import static org.opentripplanner.transit.model.filter.expr.BinaryOperator.OR;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Takes a list of matchers and provides a single interface. At least one of the matchers in the
 * list must match for the composite matcher to return a match.
 * <p/>
 * @param <T> The entity type the OrMatcher matches.
 */
public final class OrMatcher<T> implements Matcher<T> {

  private final Matcher<T>[] matchers;

  private OrMatcher(List<Matcher<T>> matchers) {
    this.matchers = matchers.toArray(Matcher[]::new);
  }

  public static <T> Matcher<T> of(Matcher<T> a, Matcher<T> b) {
    return of(List.of(a, b));
  }

  public static <T> Matcher<T> of(List<Matcher<T>> matchers) {
    // Simplify if there is just one matcher in the list
    if (matchers.size() == 1) {
      return matchers.get(0);
    }
    // Collapse nested or matchers
    var expr = new ArrayList<Matcher<T>>();
    for (Matcher<T> it : matchers) {
      if (it instanceof OrMatcher<T> orMatcher) {
        expr.addAll(Arrays.asList(orMatcher.matchers));
      } else {
        expr.add(it);
      }
    }
    return new OrMatcher<>(expr);
  }

  @Override
  public boolean match(T entity) {
    for (var m : matchers) {
      if (m.match(entity)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return "(" + OR.arrayToString(matchers) + ')';
  }
}
