package org.opentripplanner.transit.model.filter.expr;

import java.util.function.Function;

/**
 * A matcher that applies a provided matcher to an iterable of child entities returned from the main
 * entity that this matcher is for.
 * <p/>
 * If any of the iterable entities match the valueMatcher, then the match method returns true. In
 * this way it is similar to an OR.
 * <p/>
 * @param <S> The main entity type this matcher is applied to.
 * @param <T> The type of the child entities, for which there is a mapping from S to T.
 */
public class ContainsMatcher<S, T> implements Matcher<S> {

  private final String relationshipName;
  private final Function<S, Iterable<T>> valuesProvider;
  private final Matcher<T> valueMatcher;

  /**
   * @param relationshipName The name of the type of relationship between the main entity and the
   *                         entity matched by the valueMatcher.
   * @param valuesProvider The function that maps the entity being matched by this matcher (S) to
   *                       the iterable of items being matched by valueMatcher.
   * @param valueMatcher The matcher that is applied each of the iterable entities returned from the
   *                     valuesProvider function.
   */
  public ContainsMatcher(
    String relationshipName,
    Function<S, Iterable<T>> valuesProvider,
    Matcher<T> valueMatcher
  ) {
    this.relationshipName = relationshipName;
    this.valuesProvider = valuesProvider;
    this.valueMatcher = valueMatcher;
  }

  public boolean match(S entity) {
    if (valuesProvider.apply(entity) == null) {
      return false;
    }
    for (T it : valuesProvider.apply(entity)) {
      if (valueMatcher.match(it)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return "ContainsMatcher: " + relationshipName + ": " + valueMatcher;
  }
}
