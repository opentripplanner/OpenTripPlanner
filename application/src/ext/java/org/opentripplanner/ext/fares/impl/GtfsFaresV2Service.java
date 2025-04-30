package org.opentripplanner.ext.fares.impl;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.google.common.collect.Multimap;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opentripplanner.ext.fares.model.FareDistance;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.ext.fares.model.LegProducts;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.transit.model.basic.Distance;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GtfsFaresV2Service implements Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(GtfsFaresV2Service.class);
  private final List<FareLegRule> legRules;
  private final List<FareTransferRule> transferRules;
  private final Multimap<FeedScopedId, FeedScopedId> stopAreas;
  private final Set<FeedScopedId> networksWithRules;
  private final Set<FeedScopedId> fromAreasWithRules;
  private final Set<FeedScopedId> toAreasWithRules;

  public GtfsFaresV2Service(
    List<FareLegRule> legRules,
    List<FareTransferRule> fareTransferRules,
    Multimap<FeedScopedId, FeedScopedId> stopAreas
  ) {
    this.legRules = legRules;
    this.transferRules = fareTransferRules;
    this.networksWithRules = findNetworksWithRules(legRules);
    this.fromAreasWithRules = findAreasWithRules(legRules, FareLegRule::fromAreaId);
    this.toAreasWithRules = findAreasWithRules(legRules, FareLegRule::toAreaId);
    this.stopAreas = stopAreas;
  }

  public ProductResult getProducts(Itinerary itinerary) {
    var transitLegs = itinerary.listScheduledTransitLegs();

    var allLegProducts = new HashSet<LegProducts>();
    for (int i = 0; i < transitLegs.size(); i++) {
      var leg = transitLegs.get(i);
      var nextIndex = i + 1;

      Optional<ScheduledTransitLeg> nextLeg = Optional.empty();
      if (nextIndex < transitLegs.size()) {
        nextLeg = Optional.of(transitLegs.get(nextIndex));
      }

      var lp = getLegProduct(leg, nextLeg);
      allLegProducts.add(lp);
    }

    var coveringItinerary = productsCoveringItinerary(itinerary, allLegProducts);

    return new ProductResult(coveringItinerary, allLegProducts);
  }

  private static Set<FeedScopedId> findAreasWithRules(
    List<FareLegRule> legRules,
    Function<FareLegRule, FeedScopedId> getArea
  ) {
    return legRules.stream().map(getArea).filter(Objects::nonNull).collect(Collectors.toSet());
  }

  private static Set<FeedScopedId> findNetworksWithRules(Collection<FareLegRule> legRules) {
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
    var distinctProductWithTransferSets = legProducts
      .stream()
      .map(LegProducts::products)
      .collect(Collectors.toSet());

    return distinctProductWithTransferSets
      .stream()
      .flatMap(p -> p.stream().filter(ps -> coversItinerary(itinerary, ps)))
      .map(LegProducts.ProductWithTransfer::legRule)
      .flatMap(r -> r.fareProducts().stream())
      .collect(Collectors.toSet());
  }

  private boolean coversItinerary(Itinerary i, LegProducts.ProductWithTransfer pwt) {
    var transitLegs = i.listScheduledTransitLegs();
    var allLegsInProductFeed = transitLegs
      .stream()
      .allMatch(leg -> leg.getAgency().getId().getFeedId().equals(pwt.legRule().feedId()));

    return (
      allLegsInProductFeed &&
      (transitLegs.size() == 1 ||
        (pwt.products().stream().anyMatch(p -> p.coversDuration(i.totalTransitDuration())) &&
          appliesToAllLegs(pwt.legRule(), transitLegs)) ||
        coversItineraryWithFreeTransfers(i, pwt))
    );
  }

  private boolean appliesToAllLegs(FareLegRule legRule, List<ScheduledTransitLeg> transitLegs) {
    return transitLegs.stream().allMatch(leg -> legMatchesRule(leg, legRule));
  }

  private boolean coversItineraryWithFreeTransfers(
    Itinerary i,
    LegProducts.ProductWithTransfer pwt
  ) {
    var feedIdsInItinerary = i
      .listScheduledTransitLegs()
      .stream()
      .map(l -> l.getAgency().getId().getFeedId())
      .collect(Collectors.toSet());

    return (
      feedIdsInItinerary.size() == 1 &&
      pwt.transferRules().stream().anyMatch(FareTransferRule::isFree)
    );
  }

  private boolean legMatchesRule(ScheduledTransitLeg leg, FareLegRule rule) {
    // make sure that you only get rules for the correct feed
    return (
      leg.getAgency().getId().getFeedId().equals(rule.feedId()) &&
      matchesNetworkId(leg, rule) &&
      // apply only those fare leg rules which have the correct area ids
      // if area id is null, the rule applies to all legs UNLESS there is another rule that
      // covers this area
      matchesArea(leg.getFrom().stop, rule.fromAreaId(), fromAreasWithRules) &&
      matchesArea(leg.getTo().stop, rule.toAreaId(), toAreasWithRules) &&
      matchesDistance(leg, rule)
    );
  }

  private LegProducts getLegProduct(
    ScheduledTransitLeg leg,
    Optional<ScheduledTransitLeg> nextLeg
  ) {
    var legRules =
      this.legRules.stream().filter(r -> legMatchesRule(leg, r)).collect(Collectors.toSet());

    var transferRulesForLeg = transferRules
      .stream()
      .filter(t -> t.feedId().equals(leg.getAgency().getId().getFeedId()))
      .toList();

    var products = legRules
      .stream()
      .map(rule -> {
        var transferRulesToNextLeg = transferRulesForLeg
          .stream()
          .filter(GtfsFaresV2Service::checkForWildcards)
          .filter(t -> t.fromLegGroup().equals(rule.legGroupId()))
          .filter(t -> transferRuleMatchesNextLeg(nextLeg, t))
          .toList();
        return new LegProducts.ProductWithTransfer(rule, transferRulesToNextLeg);
      })
      .collect(Collectors.toSet());

    return new LegProducts(leg, nextLeg, products);
  }

  private static boolean checkForWildcards(FareTransferRule t) {
    if (Objects.isNull(t.fromLegGroup()) || Objects.isNull(t.toLegGroup())) {
      LOG.error(
        "Transfer rule {} contains a wildcard leg group reference. These are not supported yet.",
        t
      );
      return false;
    } else return true;
  }

  private boolean transferRuleMatchesNextLeg(
    Optional<ScheduledTransitLeg> nextLeg,
    FareTransferRule t
  ) {
    return nextLeg
      .map(nLeg -> {
        var maybeFareRule = getFareLegRuleByGroupId(t.toLegGroup());
        return maybeFareRule.map(rule -> legMatchesRule(nLeg, rule)).orElse(false);
      })
      .orElse(false);
  }

  private Optional<FareLegRule> getFareLegRuleByGroupId(FeedScopedId groupId) {
    return legRules.stream().filter(lr -> groupId.equals(lr.legGroupId())).findAny();
  }

  private boolean matchesArea(
    StopLocation stop,
    FeedScopedId areaId,
    Set<FeedScopedId> areasWithRules
  ) {
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
  private boolean matchesNetworkId(ScheduledTransitLeg leg, FareLegRule rule) {
    var routesNetworkIds = leg
      .getRoute()
      .getGroupsOfRoutes()
      .stream()
      .map(group -> group.getId())
      .filter(Objects::nonNull)
      .toList();

    return (
      (isNull(rule.networkId()) &&
        networksWithRules.stream().noneMatch(routesNetworkIds::contains)) ||
      routesNetworkIds.contains(rule.networkId())
    );
  }

  private boolean matchesDistance(ScheduledTransitLeg leg, FareLegRule rule) {
    // If no valid distance type is given, do not consider distances in fare computation

    FareDistance distance = rule.fareDistance();
    if (distance instanceof FareDistance.Stops(int min, int max)) {
      var numStops = leg.getIntermediateStops().size();
      return numStops >= min && max >= numStops;
    } else if (
      rule.fareDistance() instanceof FareDistance.LinearDistance(Distance min, Distance max)
    ) {
      var legDistance = leg.getDirectDistanceMeters();

      return legDistance > min.toMeters() && legDistance < max.toMeters();
    } else return true;
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
        .map(l ->
          l.products().stream().flatMap(lp -> lp.products().stream()).collect(Collectors.toSet())
        )
        .orElse(Set.of());
    }
  }
}
