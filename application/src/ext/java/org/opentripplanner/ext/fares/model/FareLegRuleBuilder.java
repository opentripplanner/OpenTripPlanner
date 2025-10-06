package org.opentripplanner.ext.fares.model;

import java.util.Collection;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Builder for {@link FareLegRule}.
 */
public class FareLegRuleBuilder {

  private final FeedScopedId id;
  private final Collection<FareProduct> fareProducts;
  private FeedScopedId legGroupId;
  private FeedScopedId networkId;
  private FeedScopedId fromAreaId;
  private FeedScopedId toAreaId;
  private FareDistance fareDistance = null;

  public FeedScopedId id() {
    return id;
  }

  public Collection<FareProduct> fareProducts() {
    return fareProducts;
  }

  public FeedScopedId legGroupId() {
    return legGroupId;
  }

  public FeedScopedId networkId() {
    return networkId;
  }

  public FeedScopedId fromAreaId() {
    return fromAreaId;
  }

  public FeedScopedId toAreaId() {
    return toAreaId;
  }

  public FareDistance fareDistance() {
    return fareDistance;
  }

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

  public FareLegRule build() {
    return new FareLegRule(this);
  }
}
