package org.opentripplanner.ext.fares.impl;

import java.util.List;
import java.util.Objects;
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
      // make sure that you only get fares for the correct feed
      .filter(legRule -> leg.getAgency().getId().getFeedId().equals(legRule.feedId()))
      // get the fare products that match the network_id
      .filter(product ->
        Objects.isNull(product.networkId()) ||
        product.networkId().equals(leg.getRoute().getNetworkId())
      )
      .map(FareLegRule::fareProduct);
  }
}
