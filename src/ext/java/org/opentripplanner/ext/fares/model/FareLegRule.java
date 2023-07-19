package org.opentripplanner.ext.fares.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.model.fare.FareProduct;

public record FareLegRule(
  @Nullable String legGroupId,
  @Nullable String networkId,
  @Nullable String fromAreaId,
  @Nullable String toAreaId,
  @Nullable FareDistance fareDistance,
  @Nonnull FareProduct fareProduct
) {
  public String feedId() {
    return fareProduct.id().getFeedId();
  }

  public static FareLegRuleBuilder of(FareProduct fp) {
    return new FareLegRuleBuilder(fp);
  }
}
