package org.opentripplanner.raptor.spi;

import javax.annotation.Nullable;

/**
 * Interface used to translate the raptor stop index to a string representation for the stop.
 */
@FunctionalInterface
public interface RaptorStopNameResolver {
  /** if resolver is null, then create a simple one and return */
  static RaptorStopNameResolver nullSafe(@Nullable RaptorStopNameResolver resolver) {
    return resolver == null ? Integer::toString : resolver;
  }

  /**
   * Translate the raptor stop index to a string representation for the stop. The returned string
   * should be human-readable and contain enough information to identify the stop in most cases.
   * <p>
   * Example: {@code "Waterloo Station(22334)"}  (name and raptor index)
   * <p>
   * The text is used for logging and debugging, and is not called during normal operation.
   */
  String apply(int stopIndex);
}
