package org.opentripplanner.ext.fares.impl;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.model.FareLegRule;
import org.opentripplanner.model.FareProduct;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ScheduledTransitLeg;

public record GtfsFaresV2Service(List<FareLegRule> legRules) {
  public Set<FareProduct> getProducts(Itinerary itinerary) {
    return itinerary
      .getTransitLegs()
      .stream()
      .flatMap(this::getLegProducts)
      .collect(Collectors.toSet());
  }

  private Stream<FareProduct> getLegProducts(ScheduledTransitLeg leg) {
    return legRules
      .stream()
      .filter(legRule -> leg.getAgency().getId().getFeedId().equals(legRule.feedId()))
      .filter(product -> product.networkId() == null)
      .map(FareLegRule::fareProduct);
  }
}
