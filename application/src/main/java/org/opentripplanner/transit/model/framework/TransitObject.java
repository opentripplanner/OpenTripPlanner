package org.opentripplanner.transit.model.framework;

import java.io.Serializable;

public interface TransitObject<E extends TransitObject<E, T>, T extends TransitBuilder<E, T>>
  extends Serializable {
  /**
   * Return {@code true} if this is the same as the given other objects, all fields must have
   * the same value. This is used to avoid creating new objects during transit model construction
   * and during RealTime updates.
   */
  boolean sameAs(E other);

  /**
   * The copy method is used to mutate the existing object by creating a builder and setting
   * {@code this} as the "original".
   * <p>
   * Note! Do not mutate nested transit entities. When crossing aggregate root boundaries, then
   * you need to fetch the entity from the transit service before changing it. Changing a nested
   * entity will cause the data to become inconsistent.
   * <p>
   * The exception is when a builder reference another builder, and not the entity object. The
   * nested entity is a <em>composed</em> part of the parent, and not shared with other entities.
   * <p>
   * TODO RTM - Document design "rules" in a package readme, when the design is set.
   */
  TransitBuilder<E, T> copy();
}
