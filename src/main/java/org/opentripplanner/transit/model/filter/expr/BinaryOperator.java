package org.opentripplanner.transit.model.filter.expr;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Used to concatenate matches with either the logical "AND" or "OR" operator.
 */
enum BinaryOperator {
  AND("&"),
  OR("|");

  private final String token;

  BinaryOperator(String token) {
    this.token = token;
  }

  @Override
  public String toString() {
    return token;
  }

  <T> String arrayToString(T[] values) {
    return colToString(Arrays.asList(values));
  }

  <T> String colToString(Collection<T> values) {
    return values.stream().map(Objects::toString).collect(Collectors.joining(" " + token + " "));
  }

  <T> String toString(T a, T b) {
    return a.toString() + " " + token + " " + b.toString();
  }
}
