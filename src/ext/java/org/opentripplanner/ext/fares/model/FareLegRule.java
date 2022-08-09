package org.opentripplanner.ext.fares.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public record FareLegRule(
  @Nullable String legGroupId,
  @Nullable String networkId,
  @Nullable String fromAreaId,
  @Nullable String toAreadId,
  @Nonnull FareProduct fareProduct
) {
  public String feedId() {
    return fareProduct.id().getFeedId();
  }
}
