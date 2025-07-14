package org.opentripplanner.ext.trias.id;

import java.util.Objects;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * An id resolver that appends a configured feed id to all input ids and strips the
 * feed id from all output ids.
 * This only works if your deployment only contains a single feed id.
 */
public class HideFeedIdResolver implements IdResolver {

  private final String feedId;

  public HideFeedIdResolver(String feedId) {
    this.feedId = Objects.requireNonNull(feedId);
  }

  @Override
  public FeedScopedId parse(String id) {
    return new FeedScopedId(feedId, id);
  }

  @Override
  public String toString(FeedScopedId id) {
    return id.getId();
  }
}
