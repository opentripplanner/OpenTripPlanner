package org.opentripplanner.ext.fares.model;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.ScheduledTransitLeg;

public record LegProducts(
  Leg leg,
  Optional<ScheduledTransitLeg> nextLeg,
  Set<ProductWithTransfer> products
) {
  public record ProductWithTransfer(FareProduct product, List<FareTransferRule> transferRules) {
    public boolean coversItinerary(Itinerary itinerary) {
      return product.coversItinerary(itinerary, transferRules);
    }
  }
}
