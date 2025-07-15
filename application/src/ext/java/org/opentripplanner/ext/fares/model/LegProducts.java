package org.opentripplanner.ext.fares.model;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.leg.ScheduledTransitLeg;

public record LegProducts(
  Leg leg,
  Optional<ScheduledTransitLeg> nextLeg,
  Set<ProductWithTransfer> products
) {
  public record ProductWithTransfer(FareLegRule legRule, List<FareTransferRule> transferRules) {
    public Collection<FareProduct> products() {
      return legRule.fareProducts();
    }
  }
}
