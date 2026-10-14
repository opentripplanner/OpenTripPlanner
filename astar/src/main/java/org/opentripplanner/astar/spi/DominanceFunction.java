package org.opentripplanner.astar.spi;

public interface DominanceFunction<State extends AStarState<State, ?, ?>> {
  boolean betterOrEqualAndComparable(State a, State b);

  default boolean comparable(State a, State b) {
    return true;
  }

  /**
   * Return true if the first state "defeats" the second state or at least ties with it in terms of
   * suitability. In the case that they are tied, we still want to return true so that an existing
   * state will kick out a new one. Provide this custom logic in subclasses. You would think this
   * could be static, but in Java for some reason calling a static function will call the one on the
   * declared type, not the runtime instance type.
   */
  default boolean betterOrEqual(State a, State b) {
    return betterOrEqualAndComparable(a, b);
  }

  default boolean significantlyBetter(State a, State b) {
    return false;
  }
}
