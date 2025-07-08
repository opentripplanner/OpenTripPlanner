package org.opentripplanner.ext.trias.id;

import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * An id resolver that passes input and output ids through with the feed id.
 */
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
