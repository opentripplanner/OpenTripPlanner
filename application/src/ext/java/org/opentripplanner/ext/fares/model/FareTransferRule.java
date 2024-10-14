package org.opentripplanner.ext.fares.model;

import java.time.Duration;
import java.util.Collection;
import javax.annotation.Nullable;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public record FareTransferRule(
  FeedScopedId id,
  @Nullable FeedScopedId fromLegGroup,
  @Nullable FeedScopedId toLegGroup,
  int transferCount,
  @Nullable Duration timeLimit,
  Collection<FareProduct> fareProducts
) {
  public String feedId() {
    return id.getFeedId();
  }
}
