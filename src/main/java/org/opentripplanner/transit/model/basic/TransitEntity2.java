package org.opentripplanner.transit.model.basic;

import java.util.List;
import javax.annotation.Nonnull;

/**
 * THIS CLASS WILL REPLACE TransitEntity AS SOON AS ALL ENTITIES ARE MIGRATED OVER TO
 * THIS CLASS INSTEAD. THEN THE SUPER CLASS TransitEntity CAN BE INLINED.
 * <p/>
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
public abstract class TransitEntity2<
  E extends TransitEntity2<E, B>, B extends TransitEntityBuilder<E, B>
>
  extends TransitEntity
  implements TransitObject<E, B> {

  public TransitEntity2(@Nonnull FeedScopedId id) {
    super(id);
  }

  protected static <T> List<T> listOfNullSafe(List<T> list) {
    if (list == null || list.isEmpty()) {
      return List.of();
    }
    return List.copyOf(list);
  }
}
