package org.opentripplanner.transit.model.framework;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * All OTP Transit entities should extend this class. The purpose of the class is to enforce a
 * common implementation of the identity:
 * <ol>
 *   <li>
 *     {@code id} - All entities should have an id. The id should be unique within the
 *     context the entity live. For aggregates which live in a global space the id must be unique.
 *     This apply to all entities listed in the index service.
 *   </li>
 *   <li>
 *     The {@code hashCode()/equals()} method is consistent and based on the id (identity). We
 *     frequently use this to index and lookup entities.
 *   </li>
 * </ol>
 * This class also enforce a strong type-safe relationship between entity and builder.
 */
public abstract class AbstractTransitEntity<
  E extends AbstractTransitEntity<E, B>, B extends AbstractEntityBuilder<E, B>
>
  implements TransitEntity, TransitObject<E, B>, Serializable {

  private final FeedScopedId id;

  public AbstractTransitEntity(FeedScopedId id) {
    this.id = Objects.requireNonNull(id);
  }

  public final FeedScopedId getId() {
    return id;
  }

  @Override
  public final int hashCode() {
    return id.hashCode();
  }

  /**
   * Uses the  {@code id} for identity. We could use the {@link Object#equals(Object)} method, but
   * this causes the equals to fail in cases were the same entity is created twice - for example
   * after reloading a serialized instance.
   */
  @Override
  public final boolean equals(Object obj) {
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    AbstractTransitEntity<E, B> other = (AbstractTransitEntity<E, B>) obj;
    return getId().equals(other.getId());
  }

  /**
   * Provide a default toString implementation with class name and id.
   */
  @Override
  public final String toString() {
    var buf = new StringBuilder(getClass().getSimpleName());

    buf.append('{').append(getId());

    if ((this instanceof LogInfo n) && n.logName() != null) {
      buf.append(' ').append(n.logName());
    }
    buf.append('}');
    return buf.toString();
  }

  protected static <T> List<T> listOfNullSafe(@Nullable List<T> list) {
    if (list == null || list.isEmpty()) {
      return List.of();
    }
    return List.copyOf(list);
  }

  protected static <T> Set<T> setOfNullSafe(@Nullable Collection<T> input) {
    if (input == null || input.isEmpty()) {
      return Set.of();
    }
    return Set.copyOf(input);
  }
}
