package org.opentripplanner.ext.fares.impl.gtfs;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.plan.Leg;

record FareProductMatch(
  Leg leg,
  Set<ProductWithTransfer> products,
  Set<Transfer> transfersFromPreviousLeg
) {
  public Set<FareProduct> fareProducts() {
    return products
      .stream()
      .flatMap(lp -> lp.products().stream())
      .collect(Collectors.toUnmodifiableSet());
  }

  public record ProductWithTransfer(FareLegRule legRule, List<FareTransferRule> transferRules) {
    public Collection<FareProduct> products() {
      return legRule.fareProducts();
    }
  }

  public record Transfer(FareProduct product) {}
}
