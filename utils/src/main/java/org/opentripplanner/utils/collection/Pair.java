package org.opentripplanner.utils.collection;

import java.util.Objects;

/**
 * A set with exactly two elements
 */
public record Pair<T>(T first, T second) {
  public Pair {
    Objects.requireNonNull(first);
    Objects.requireNonNull(second);
  }
}
