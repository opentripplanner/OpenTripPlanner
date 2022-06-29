package org.opentripplanner.ext.fares.impl;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.model.FareLegRule;
import org.opentripplanner.model.FareProduct;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.fares.FareService;

public record GtfsFaresV2Service(List<FareLegRule> legRules) implements FareService {
  @Override
  public Fare getCost(Itinerary itinerary) {
    var fare = new Fare();
    var products = itinerary
      .getTransitLegs()
      .stream()
      .flatMap(this::getLegProducts)
      .collect(Collectors.toSet());
    fare.addProducts(products);
    return fare;
  }

  private Stream<FareProduct> getLegProducts(ScheduledTransitLeg leg) {
    return legRules
      .stream()
      .filter(legRule -> leg.getAgency().getId().getFeedId().equals(legRule.feedId()))
      .filter(product -> product.networkId() == null)
      .map(FareLegRule::fareProduct);
  }
}
