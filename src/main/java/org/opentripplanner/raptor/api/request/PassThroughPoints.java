package org.opentripplanner.raptor.api.request;

import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import javax.annotation.Nullable;

/**
 * Provides information for pass-through points used in the search.
 */
public interface PassThroughPoints {
  /** Implementation that answers negative for all stops. */
  PassThroughPoints NOOP = new PassThroughPoints() {
    @Override
    public boolean isPassThroughPoint(final int stop) {
      return false;
    }

    @Override
    public void updateC2Value(int currentPathC2, IntConsumer update) {
      throw new UnsupportedOperationException();
    }

    @Override
    public IntPredicate acceptC2AtDestination() {
      return null;
    }
  };

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
   * Return a predicate witch can be used to reject paths with an undesired c2 value. This is
   * used by Raptor to reject destination arrivals.
   * <p>
   * This method MUST return {@code null} if the mc-raptor do not use a second-criteria(c2).
   * The reason for this is that the presence of this function will cause Raptor to get the
   * c2 value - witch may not exist; hence throw an exception.
   */
  @Nullable
  IntPredicate acceptC2AtDestination();
}
