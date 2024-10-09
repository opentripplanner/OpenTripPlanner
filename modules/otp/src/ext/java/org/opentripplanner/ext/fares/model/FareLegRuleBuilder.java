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
  private String networkId;
  private String fromAreaId;
  private FareDistance fareDistance = null;
  private String toAreaId;

  public FareLegRuleBuilder(FeedScopedId id, Collection<FareProduct> products) {
    this.id = id;
    this.fareProducts = products;
  }

  public FareLegRuleBuilder withLegGroupId(FeedScopedId legGroupId) {
    this.legGroupId = legGroupId;
    return this;
  }

  public FareLegRuleBuilder withNetworkId(String networkId) {
    this.networkId = networkId;
    return this;
  }

  public FareLegRuleBuilder withFromAreaId(String fromAreaId) {
    this.fromAreaId = fromAreaId;
    return this;
  }

  public FareLegRuleBuilder withFareDistance(FareDistance fareDistance) {
    this.fareDistance = fareDistance;
    return this;
  }

  public FareLegRuleBuilder withToAreaId(String toAreaId) {
    this.toAreaId = toAreaId;
    return this;
  }

  public FareLegRule build() {
    return new FareLegRule(
      id,
      legGroupId,
      networkId,
      fromAreaId,
      toAreaId,
      fareDistance,
      fareProducts
    );
  }
}
