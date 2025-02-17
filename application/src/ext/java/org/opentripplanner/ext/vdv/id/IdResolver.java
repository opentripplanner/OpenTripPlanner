package org.opentripplanner.ext.vdv.id;

import org.opentripplanner.transit.model.framework.FeedScopedId;

public interface IdResolver {
  FeedScopedId parse(String id);

  String toString(FeedScopedId id);
}
