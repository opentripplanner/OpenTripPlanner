package org.opentripplanner.api.model.transit;

import org.opentripplanner.core.model.id.FeedScopedId;

/**
 * An id mapper that passes input and output ids through with the feed id.
 */
public class DefaultFeedIdMapper implements FeedScopedIdMapper {

  @Override
  public FeedScopedId parse(String id) {
    return FeedScopedId.parse(id);
  }

  @Override
  public String mapToApi(FeedScopedId id) {
    return id.toString();
  }
}
