package org.opentripplanner.model.fare;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.framework.lang.Sandbox;

@Sandbox
public record FareLegRule(
  @Nullable String legGroupId,
  @Nullable String networkId,
  @Nullable String fromAreaId,
  @Nullable String toAreadId,
  @Nullable FareDistance fareDistance,
  @Nonnull FareProduct fareProduct
) {
  public FareLegRule(
    String legGroupId,
    String networkId,
    String fromAreaId,
    String toAreaId,
    FareProduct fareProduct
  ) {
    this(legGroupId, networkId, fromAreaId, toAreaId, null, fareProduct);
  }
  public String feedId() {
    return fareProduct.id().getFeedId();
  }
}
