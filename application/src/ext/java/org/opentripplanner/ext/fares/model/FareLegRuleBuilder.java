package org.opentripplanner.ext.fares.model;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.fare.FareProduct;

/**
 * Builder for {@link FareLegRule}.
 */
public class FareLegRuleBuilder {

  final FeedScopedId id;
  final Collection<FareProduct> fareProducts;
  FeedScopedId legGroupId;
  FeedScopedId networkId;
  FeedScopedId fromAreaId;
  FeedScopedId toAreaId;
  FareDistance fareDistance = null;

  @Nullable
  Integer priority;

  Collection<Timeframe> fromTimeframes = List.of();
  Collection<Timeframe> toTimeframes = List.of();

  public FareLegRuleBuilder(FeedScopedId id, Collection<FareProduct> products) {
    this.id = id;
    this.fareProducts = products;
  }

  public FareLegRuleBuilder withLegGroupId(FeedScopedId legGroupId) {
    this.legGroupId = legGroupId;
    return this;
  }

  public FareLegRuleBuilder withNetworkId(FeedScopedId networkId) {
    this.networkId = networkId;
    return this;
  }

  public FareLegRuleBuilder withFromAreaId(FeedScopedId fromAreaId) {
    this.fromAreaId = fromAreaId;
    return this;
  }

  public FareLegRuleBuilder withFareDistance(FareDistance fareDistance) {
    this.fareDistance = fareDistance;
    return this;
  }

  public FareLegRuleBuilder withToAreaId(FeedScopedId toAreaId) {
    this.toAreaId = toAreaId;
    return this;
  }

  public FareLegRuleBuilder withPriority(Integer priority) {
    this.priority = priority;
    return this;
  }

  public FareLegRuleBuilder withFromTimeframes(Collection<Timeframe> timeFrames) {
    this.fromTimeframes = timeFrames;
    return this;
  }

  public FareLegRuleBuilder withToTimeframes(Collection<Timeframe> timeFrames) {
    this.toTimeframes = timeFrames;
    return this;
  }

  public FareLegRule build() {
    return new FareLegRule(this);
  }
}
