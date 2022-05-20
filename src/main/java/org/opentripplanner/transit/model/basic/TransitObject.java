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

  /**
   * The copy method is used to mutate the existing object by creating a builder and setting
   * the "original".
   * <p>
   * Note! Do not mutate nested transit entities. When crossing aggregate root boundaries, then
   * you need to fetch the entity from the transit service before changing it. Changing a nested
   * entity will cause the data to become inconsistent. For example:
   * <pre>
   *
   * Trip t = (get from transit service)
   *
   * // This is NOT OK - the new agency is not consistent with the agency in the transit service
   * var a = t.getAgency().copy().withXYZ(...).build();
   * t.setAgency(a);
   *
   * // Instead
   * Trip t = (get from transit service)
   * Agency a = (get from transit service)
   *
   * // This is ok
   * a = a.copy().withXYZ(...).build();
   * t.copy().withAgency(a).build();
   * </pre>
   *
   * The exception is when a builder reference another builder, and not the entity object. The
   * nested entity is a <em>composed</em> part of the parent, and not shared with other entities.
   */
  AbstractBuilder<E, T> copy();
}
