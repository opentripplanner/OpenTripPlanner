package org.opentripplanner.model.fare;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.opentripplanner.framework.lang.Sandbox;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.ScheduledTransitLeg;

@Sandbox
public record LegProducts(
  Leg leg,
  Optional<ScheduledTransitLeg> nextLeg,
  Set<ProductWithTransfer> products
) {
  public record ProductWithTransfer(FareLegRule legRule, List<FareTransferRule> transferRules) {
    public FareProduct product() {
      return legRule.fareProduct();
    }
  }
}
