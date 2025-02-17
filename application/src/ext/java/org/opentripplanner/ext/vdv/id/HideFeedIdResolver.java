package org.opentripplanner.ext.vdv.id;

import java.util.Objects;
import org.opentripplanner.transit.model.framework.FeedScopedId;

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
