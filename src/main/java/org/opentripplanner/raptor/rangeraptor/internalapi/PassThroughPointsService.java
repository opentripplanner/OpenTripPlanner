package org.opentripplanner.raptor.rangeraptor.internalapi;

import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import org.opentripplanner.raptor.api.model.DominanceFunction;

/**
 * Provides information for pass-through points used in the search.
 */
public interface PassThroughPointsService {
  /** Implementation that answers negative for all stops. */
  PassThroughPointsService NOOP = new PassThroughPointsService() {
    @Override
    public boolean isPassThroughPoint(final int stop) {
      return false;
    }

    @Override
    public void updateC2Value(int currentPathC2, IntConsumer update) {
      throw new UnsupportedOperationException();
    }

    @Override
    public DominanceFunction dominanceFunction() {
      throw new UnsupportedOperationException();
    }

    @Override
    public IntPredicate acceptC2AtDestination() {
      throw new UnsupportedOperationException();
    }
  };

  /**
   * No pass-through service is present, just the noop version.
   */
  default boolean isNoop() {
    return this == NOOP;
  }

  /**
   * If a certain stop is a pass-through point of a certain position in the trip.
   *
   * @param stopIndex the stop index to check
   * @return boolean true if the stop is a pass-through point on the specific stop
   */
  boolean isPassThroughPoint(int stopIndex);

  /**
   * If the {@link #isPassThroughPoint(int)} is {@code true} Raptor will call this method to inject
   * the new {@code c2} value for the current path. The implementation must verify the
   * {@code currentPathC2} value, and ONLY call the {@code update} callback if a new value exist.
   */
  void updateC2Value(int currentPathC2, IntConsumer update);

  /**
   * This is the dominance function to use for comparing transit-group-priorityIds.
   * It is critical that the implementation is "static" so it can be inlined, since it
   * is run in the innermost loop of Raptor.
   */
  DominanceFunction dominanceFunction();

  /**
   * Return a predicate which can be used to reject paths with an undesired c2 value. This is
   * used by Raptor to reject destination arrivals.
   */
  IntPredicate acceptC2AtDestination();
}
