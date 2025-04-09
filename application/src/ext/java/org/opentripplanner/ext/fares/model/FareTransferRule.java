package org.opentripplanner.ext.fares.model;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public record FareTransferRule(
  FeedScopedId id,
  @Nullable FeedScopedId fromLegGroup,
  @Nullable FeedScopedId toLegGroup,
  int transferCount,
  @Nullable Duration timeLimit,
  Collection<FareProduct> fareProducts
) {
  public FareTransferRule {
    Objects.requireNonNull(id);
    fareProducts = List.copyOf(fareProducts);
  }
  public String feedId() {
    return id.getFeedId();
  }

  /**
   * Returns true if this rule contains a free transfer product or an empty list of fare products.
   */
  public boolean isFree() {
    return fareProducts.isEmpty() || fareProducts.stream().allMatch(p -> p.price().isZero());
  }

  public boolean containsWildCard() {
    return fromLegGroup == null || toLegGroup == null;
  }

  public Collection<FareProduct> fareProducts() {
    if (isFree()) {
      return List.of(
        FareProduct.of(
          new FeedScopedId(id.getFeedId(), "free"),
          "free transfer",
          Money.ZERO_USD
        ).build()
      );
    } else {
      return fareProducts;
    }
  }
}
