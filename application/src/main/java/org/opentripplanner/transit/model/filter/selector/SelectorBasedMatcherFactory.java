package org.opentripplanner.transit.model.filter.selector;

import java.util.List;
import java.util.function.Function;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.model.filter.expr.ExpressionBuilder;
import org.opentripplanner.transit.model.filter.expr.Matcher;
import org.opentripplanner.transit.model.filter.expr.OrMatcher;

public class SelectorBasedMatcherFactory {

  /**
   * Creates a matcher from a list of {@link FilterRequest} filters.
   * A T matches if it matches at least one of the filters (OR between filters).
   *
   * <br>
   * Each filter implements select/not semantics, meaning each selector will:
   * <ul>
   *   <li>Match at least one select criterion (or all if select is null), AND</li>
   *   <li>Match none of the not criteria.</li>
   * </ul>
   */
  public static <T, TSelectRequest> Matcher<T> of(
    List<FilterRequest<TSelectRequest>> filters,
    Function<TSelectRequest, Matcher<T>> selectorMatcherProvider
  ) {
    List<Matcher<T>> filterMatchers = filters
      .stream()
      .map(filter -> buildFilterMatcher(filter, selectorMatcherProvider))
      .toList();

    return OrMatcher.of(filterMatchers);
  }

  /**
   * Builds a matcher for a single filter request implementing select/not semantics:
   * <ul>
   *   <li>Match at least one select criterion (or all if select is null), AND</li>
   *   <li>Match none of the not criteria.</li>
   * </ul>
   */
  private static <T, TSelectRequest> Matcher<T> buildFilterMatcher(
    FilterRequest<TSelectRequest> filter,
    Function<TSelectRequest, Matcher<T>> buildSelectorMatcher
  ) {
    return ExpressionBuilder.<T>of()
      .atLeastOneMatch(
        FilterValues.ofNullIsEverything("select", filter.select()),
        buildSelectorMatcher
      )
      .matchesNone(FilterValues.ofNullIsEverything("not", filter.not()), buildSelectorMatcher)
      .build();
  }
}
