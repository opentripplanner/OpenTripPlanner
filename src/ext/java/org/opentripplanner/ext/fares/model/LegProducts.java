package org.opentripplanner.ext.fares.model;

import java.util.List;
import java.util.Set;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;

public record LegProducts(Leg leg, Set<ProductWithTransfer> products) {
  public record ProductWithTransfer(FareProduct product, List<FareTransferRule> transferRules) {
    public boolean coversItinerary(Itinerary itinerary) {
      return product.coversItinerary(itinerary, transferRules);
    }
  }
}
