package org.opentripplanner.transit.model.basic;

import java.io.Serializable;
import javax.annotation.Nonnull;

public interface TransitObject<E extends TransitObject<E, T>, T extends AbstractBuilder<E, T>>
  extends Serializable {
  /**
   * Return {@code true} if this is the same as the given other objects, all fields must have
   * the same value. This is used to avoid creating new objects during transit model construction
   * and during RealTime updates.
   * <p>
   * Make sure to implement {@link #toString()}, {@link #equals(Object)} and {@link #hashCode()}
   * as well.
   */
  boolean sameValue(@Nonnull E other);

  AbstractBuilder<E, T> copy();
}
