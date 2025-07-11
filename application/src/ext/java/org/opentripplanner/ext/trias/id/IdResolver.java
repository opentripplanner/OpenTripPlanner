package org.opentripplanner.ext.trias.id;

import javax.annotation.Nullable;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Handles mapping from external IDs into feed-scoped ones.
 */
public interface IdResolver {
  // ToDo: relocate to somewhere outside of trias package
  FeedScopedId parse(String id);

  FeedScopedId parseNullSafe(@Nullable String id);

  String toString(FeedScopedId id);
}
