package org.opentripplanner.ext.fares.model;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public record FareLegRule(
  @Nonnull FeedScopedId id,
  @Nullable FeedScopedId legGroupId,
  @Nullable String networkId,
  @Nullable String fromAreaId,
  @Nullable String toAreaId,
  @Nullable FareDistance fareDistance,
  @Nonnull Collection<FareProduct> fareProducts
) {
  public FareLegRule {
    Objects.requireNonNull(id);
    Objects.requireNonNull(fareProducts);
    if (fareProducts.isEmpty()) {
      throw new IllegalArgumentException("fareProducts must contain at least one value");
    }
    fareProducts.forEach(Objects::requireNonNull);
  }

  public String feedId() {
    return id.getFeedId();
  }

  public static FareLegRuleBuilder of(FeedScopedId id, FareProduct fp) {
    return new FareLegRuleBuilder(id, List.of(fp));
  }
  public static FareLegRuleBuilder of(FeedScopedId id, Collection<FareProduct> fp) {
    return new FareLegRuleBuilder(id, fp);
  }
}
