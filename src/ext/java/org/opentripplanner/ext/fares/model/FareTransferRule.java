package org.opentripplanner.ext.fares.model;

import java.time.Duration;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.model.fare.FareProduct;

public record FareTransferRule(
  @Nullable String fromLegGroup,
  @Nullable String toLegGroup,
  int transferCount,
  @Nullable Duration timeLimit,
  @Nonnull FareProduct fareProduct
) {
  public String feedId() {
    return fareProduct.id().getFeedId();
  }
}
