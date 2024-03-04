package org.opentripplanner.transit.model.filter.expr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

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

  public <V> ExpressionBuilder<T> or(Collection<V> values, Function<V, Matcher<T>> valueProvider) {
    if (values.isEmpty()) {
      return this;
    }

    matchers.add(OrMatcher.of(values.stream().map(valueProvider).toList()));
    return this;
  }

  public Matcher<T> build() {
    return AndMatcher.of(matchers);
  }
}
