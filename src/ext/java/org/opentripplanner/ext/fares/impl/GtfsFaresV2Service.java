package org.opentripplanner.ext.fares.impl;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.opentripplanner.model.FareLegRule;
import org.opentripplanner.model.FareProduct;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ScheduledTransitLeg;

public record GtfsFaresV2Service(List<FareLegRule> legRules) {
  public ProductResult getProducts(Itinerary itinerary) {
    var coveringItinerary = itinerary
      .getTransitLegs()
      .stream()
      .flatMap(this::getLegProducts)
      .filter(p -> p.coversItinerary(itinerary))
      .distinct()
      .toList();

    return new ProductResult(coveringItinerary);
  }

  private Stream<FareProduct> getLegProducts(ScheduledTransitLeg leg) {
    return legRules
      .stream()
      // make sure that you only get rules for the correct feed
      .filter(legRule -> leg.getAgency().getId().getFeedId().equals(legRule.feedId()))
      // get the fare products that match the network_id
      .filter(product ->
        Objects.isNull(product.networkId()) ||
        product.networkId().equals(leg.getRoute().getNetworkId())
      )
      .map(FareLegRule::fareProduct);
  }

  public record ProductResult(List<FareProduct> productsCoveringItinerary) {}
}
