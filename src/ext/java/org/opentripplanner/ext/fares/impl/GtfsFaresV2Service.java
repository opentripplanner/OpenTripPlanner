package org.opentripplanner.ext.fares.impl;

import com.google.common.collect.Multimap;
import java.io.Serializable;
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
import org.opentripplanner.transit.model.framework.FeedScopedId;

public final class GtfsFaresV2Service implements Serializable {

  private final List<FareLegRule> legRules;
  private final Multimap<FeedScopedId, String> stopAreas;
  private final Set<String> networksWithRules;
  private final Set<String> stopAreasWithRules;

  public GtfsFaresV2Service(List<FareLegRule> legRules, Multimap<FeedScopedId, String> stopAreas) {
    this.legRules = legRules;
    this.networksWithRules = findNetworksWithRules(legRules);
    this.stopAreasWithRules = findAreasWithRules(legRules);
    this.stopAreas = stopAreas;
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

  private static Set<String> findAreasWithRules(List<FareLegRule> legRules) {
    return legRules
      .stream()
      .flatMap(rule -> Stream.of(rule.fromAreaId(), rule.toAreadId()).filter(Objects::nonNull))
      .collect(Collectors.toSet());
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
      .filter(rule -> filterByNetworkId(leg, rule))
      // apply only those rules which have the correct area ids
      .filter(rule -> filterByFromArea(leg, rule))
      .map(FareLegRule::fareProduct);
  }

  private boolean filterByFromArea(ScheduledTransitLeg leg, FareLegRule rule) {
    var fromStopAreas = stopAreas.get(leg.getFrom().stop.getId());
    return (
      (
        Objects.isNull(rule.fromAreaId()) &&
        fromStopAreas.stream().noneMatch(stopAreasWithRules::contains)
      ) ||
      (Objects.nonNull(rule.fromAreaId()) && fromStopAreas.contains(rule.fromAreaId()))
    );
  }

  /**
   * Get the fare products that match the network_id. If the network id of the product is null it
   * depends on the presence/absence of other rules with that network id.
   */
  private boolean filterByNetworkId(ScheduledTransitLeg leg, FareLegRule rule) {
    return (
      (
        Objects.isNull(rule.networkId()) &&
        !networksWithRules.contains(leg.getRoute().getNetworkId())
      ) ||
      Objects.equals(rule.networkId(), leg.getRoute().getNetworkId())
    );
  }
}

record ProductResult(List<FareProduct> productsCoveringItinerary) {}
