package org.opentripplanner.ext.fares.model;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.collection.CollectionUtils;

public record FareLegRule(
  FeedScopedId id,
  @Nullable FeedScopedId legGroupId,
  @Nullable FeedScopedId networkId,
  @Nullable FeedScopedId fromAreaId,
  @Nullable FeedScopedId toAreaId,
  @Nullable FareDistance fareDistance,
  Collection<FareProduct> fareProducts
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
