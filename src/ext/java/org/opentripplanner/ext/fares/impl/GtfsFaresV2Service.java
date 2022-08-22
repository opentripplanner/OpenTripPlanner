package org.opentripplanner.ext.fares.impl;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.google.common.collect.Multimap;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareProduct;
import org.opentripplanner.ext.fares.model.LegProducts;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.GroupOfRoutes;
import org.opentripplanner.transit.model.site.StopLocation;

public final class GtfsFaresV2Service implements Serializable {

  private final List<FareLegRule> legRules;
  private final Multimap<FeedScopedId, String> stopAreas;
  private final Set<String> networksWithRules;
  private final Set<String> fromAreasWithRules;
  private final Set<String> toAreasWithRules;

  public GtfsFaresV2Service(List<FareLegRule> legRules, Multimap<FeedScopedId, String> stopAreas) {
    this.legRules = legRules;
    this.networksWithRules = findNetworksWithRules(legRules);
    this.fromAreasWithRules = findAreasWithRules(legRules, FareLegRule::fromAreaId);
    this.toAreasWithRules = findAreasWithRules(legRules, FareLegRule::toAreadId);
    this.stopAreas = stopAreas;
  }

  public ProductResult getProducts(Itinerary itinerary) {
    var legProducts = itinerary
      .getScheduledTransitLegs()
      .stream()
      .map(this::getLegProduct)
      .filter(lp -> !lp.products().isEmpty())
      .collect(Collectors.toSet());

    var coveringItinerary = productsCoveringItinerary(itinerary, legProducts);

    return new ProductResult(coveringItinerary, legProducts);
  }

  private static Set<String> findAreasWithRules(
    List<FareLegRule> legRules,
    Function<FareLegRule, String> getArea
  ) {
    return legRules.stream().map(getArea).filter(Objects::nonNull).collect(Collectors.toSet());
  }

  private static Set<String> findNetworksWithRules(Collection<FareLegRule> legRules) {
    return legRules
      .stream()
      .map(FareLegRule::networkId)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
  }

  private Set<FareProduct> productsCoveringItinerary(
    Itinerary itinerary,
    Collection<LegProducts> legProducts
  ) {
    var distinctProductSets = legProducts
      .stream()
      .map(LegProducts::products)
      .collect(Collectors.toSet());

    if (distinctProductSets.size() <= 1) {
      return distinctProductSets
        .stream()
        .flatMap(p -> p.stream().filter(ps -> ps.coversItinerary(itinerary)))
        .collect(Collectors.toSet());
    } else {
      return Set.of();
    }
  }

  private LegProducts getLegProduct(ScheduledTransitLeg leg) {
    var products = legRules
      .stream()
      // make sure that you only get rules for the correct feed
      .filter(legRule -> leg.getAgency().getId().getFeedId().equals(legRule.feedId()))
      .filter(rule -> filterByNetworkId(leg, rule))
      // apply only those fare leg rules which have the correct area ids
      // if area id is null, the rule applies to all legs UNLESS there is another rule that
      // covers this area
      .filter(rule -> filterByArea(leg.getFrom().stop, rule.fromAreaId(), fromAreasWithRules))
      .filter(rule -> filterByArea(leg.getTo().stop, rule.toAreadId(), toAreasWithRules))
      .map(FareLegRule::fareProduct)
      .collect(Collectors.toSet());
    return new LegProducts(leg, products);
  }

  private boolean filterByArea(StopLocation stop, String areaId, Set<String> areasWithRules) {
    var stopAreas = this.stopAreas.get(stop.getId());
    return (
      (isNull(areaId) && stopAreas.stream().noneMatch(areasWithRules::contains)) ||
      (nonNull(areaId) && stopAreas.contains(areaId))
    );
  }

  /**
   * Get the fare products that match the network_id. If the network id of the product is null it
   * depends on the presence/absence of other rules with that network id.
   */
  private boolean filterByNetworkId(ScheduledTransitLeg leg, FareLegRule rule) {
    var routesNetworkIds = leg
      .getRoute()
      .getGroupsOfRoutes()
      .stream()
      .map(group -> group.getId().getId())
      .filter(Objects::nonNull)
      .toList();

    return (
      (
        isNull(rule.networkId()) && networksWithRules.stream().noneMatch(routesNetworkIds::contains)
      ) ||
      routesNetworkIds.contains(rule.networkId())
    );
  }
}

/**
 * @param itineraryProducts The fare products that cover the entire itinerary, like a daily pass.
 * @param legProducts       The fare products that cover only individual legs.
 */
record ProductResult(Set<FareProduct> itineraryProducts, Set<LegProducts> legProducts) {
  public Set<FareProduct> getProducts(Leg leg) {
    return legProducts
      .stream()
      .filter(lp -> lp.leg().equals(leg))
      .findFirst()
      .map(LegProducts::products)
      .orElse(Set.of());
  }
}
