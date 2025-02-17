package org.opentripplanner.ext.vdv.id;

import org.opentripplanner.transit.model.framework.FeedScopedId;

public class UseFeedIdResolver implements IdResolver {

  @Override
  public FeedScopedId parse(String id) {
    return FeedScopedId.parse(id);
  }

  @Override
  public String toString(FeedScopedId id) {
    return id.toString();
  }
}
