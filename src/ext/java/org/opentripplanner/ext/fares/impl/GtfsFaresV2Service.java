package org.opentripplanner.ext.fares.impl;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.model.FareLegRule;
import org.opentripplanner.model.FareProduct;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ScheduledTransitLeg;

public final class GtfsFaresV2Service {

  private final List<FareLegRule> legRules;
  private final Set<String> networksWithRules;

  public GtfsFaresV2Service(List<FareLegRule> legRules) {
    this.legRules = legRules;
    this.networksWithRules = findNetworksWithRules(legRules);
  }

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

  private static Set<String> findNetworksWithRules(Collection<FareLegRule> legRules) {
    return legRules
      .stream()
      .map(FareLegRule::networkId)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
  }

  private Stream<FareProduct> getLegProducts(ScheduledTransitLeg leg) {
    return legRules
      .stream()
      // make sure that you only get rules for the correct feed
      .filter(legRule -> leg.getAgency().getId().getFeedId().equals(legRule.feedId()))
      // get the fare products that match the network_id
      // the the network id of the product is null it depends on the presence/absence of other rules
      // with that network id
      .filter(product ->
        (
          Objects.isNull(product.networkId()) &&
          !networksWithRules.contains(leg.getRoute().getNetworkId())
        ) ||
        Objects.equals(product.networkId(), leg.getRoute().getNetworkId())
      )
      .map(FareLegRule::fareProduct);
  }
}

record ProductResult(List<FareProduct> productsCoveringItinerary) {}
