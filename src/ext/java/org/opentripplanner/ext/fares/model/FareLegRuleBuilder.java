package org.opentripplanner.ext.fares.model;

import org.opentripplanner.model.fare.FareProduct;

/**
 * Builder for {@link FareLegRule}.
 */
public class FareLegRuleBuilder {

  private final FareProduct fareProduct;
  private String legGroupId;
  private String networkId;
  private String fromAreaId;
  private FareDistance fareDistance = null;
  private String toAreaId;

  public FareLegRuleBuilder(FareProduct product) {
    this.fareProduct = product;
  }

  public FareLegRuleBuilder withLegGroupId(String legGroupId) {
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
    return new FareLegRule(legGroupId, networkId, fromAreaId, toAreaId, fareDistance, fareProduct);
  }
}
