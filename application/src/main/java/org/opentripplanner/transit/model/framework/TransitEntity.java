package org.opentripplanner.transit.model.framework;

import java.util.Comparator;
import org.opentripplanner.core.model.id.FeedScopedId;

public interface TransitEntity {
  FeedScopedId getId();

  /**
   * Compare transit entities by their {@link FeedScopedId}. Useful for achieving deterministic
   * ordering, but the sort order itself will rarely carry semantic meaning and so usually shouldn't
   * be relied on.
   */
  static <T extends AbstractTransitEntity<?, ?>> Comparator<T> idComparator() {
    return Comparator.comparing(AbstractTransitEntity::getId);
  }
}
