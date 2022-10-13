package org.opentripplanner.ext.fares.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
