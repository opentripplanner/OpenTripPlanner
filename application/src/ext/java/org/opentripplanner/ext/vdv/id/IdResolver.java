package org.opentripplanner.ext.vdv.id;

import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Handles mapping from external IDs into feed-scoped ones.
 */
public interface IdResolver {
  FeedScopedId parse(String id);

  String toString(FeedScopedId id);
}
