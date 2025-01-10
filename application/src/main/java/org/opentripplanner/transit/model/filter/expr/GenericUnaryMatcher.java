package org.opentripplanner.transit.model.filter.expr;

import java.util.function.Predicate;

/**
 * A generic matcher that takes a predicate function that returns a boolean given the matched type.
 * <p/>
 * @param <T> The type of the entity being matched.
 */
public class GenericUnaryMatcher<T> implements Matcher<T> {

  private final String typeName;
  private final Predicate<T> matchPredicate;

  /**
   * @param typeName The typeName appears in the toString for easier debugging.
   * @param matchPredicate The predicate that will be used to test the entity being matched.
   */
  public GenericUnaryMatcher(String typeName, Predicate<T> matchPredicate) {
    this.typeName = typeName;
    this.matchPredicate = matchPredicate;
  }

  @Override
  public boolean match(T entity) {
    return matchPredicate.test(entity);
  }

  @Override
  public String toString() {
    return "GenericUnaryMatcher: " + typeName;
  }
}
