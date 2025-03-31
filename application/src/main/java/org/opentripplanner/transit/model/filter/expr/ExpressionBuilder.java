package org.opentripplanner.transit.model.filter.expr;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.opentripplanner.transit.api.model.FilterValues;

/**
 * A builder for creating complex matchers composed of other matchers.
 * <p/>
 * This builder contains convenience methods for creating complex matchers from simpler ones. The
 * resulting matcher "ands" together all the matchers it has built up. This supports the common
 * pattern of narrowing results with multiple filters.
 *
 * @param <T> The type of entity to match in the expression.
 */
public class ExpressionBuilder<T> {

  private final List<Matcher<T>> matchers = new ArrayList<>();

  public static <T> ExpressionBuilder<T> of() {
    return new ExpressionBuilder<>();
  }

  public ExpressionBuilder<T> matches(Matcher<T> matcher) {
    matchers.add(matcher);
    return this;
  }

  public <V> ExpressionBuilder<T> atLeastOneMatch(
    FilterValues<V> filterValues,
    Function<V, Matcher<T>> matcherProvider
  ) {
    if (filterValues.includeEverything()) {
      return this;
    }

    matchers.add(OrMatcher.of(filterValues.get().stream().map(matcherProvider).toList()));
    return this;
  }

  public <V> ExpressionBuilder<T> matchesNone(
    FilterValues<V> filterValues,
    Function<V, Matcher<T>> matcherProvider
  ) {
    if (filterValues.includeEverything()) {
      return this;
    }
    matchers.add(
      new NegationMatcher<>(
        "matchesNone",
        OrMatcher.of(filterValues.get().stream().map(matcherProvider).toList())
      )
    );
    return this;
  }

  public Matcher<T> build() {
    return AndMatcher.of(matchers);
  }
}
