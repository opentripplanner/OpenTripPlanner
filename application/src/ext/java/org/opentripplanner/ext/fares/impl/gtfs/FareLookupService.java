package org.opentripplanner.ext.fares.impl.gtfs;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.google.common.collect.Multimap;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opentripplanner.ext.fares.model.FareDistance;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.transit.model.basic.Distance;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FareLookupService implements Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(FareLookupService.class);
  private final List<FareLegRule> legRules;
  private final List<FareTransferRule> transferRules;
  private final Multimap<FeedScopedId, FeedScopedId> stopAreas;
  private final Set<FeedScopedId> networksWithRules;
  private final Set<FeedScopedId> fromAreasWithRules;
  private final Set<FeedScopedId> toAreasWithRules;

  FareLookupService(
    List<FareLegRule> legRules,
    List<FareTransferRule> fareTransferRules,
    Multimap<FeedScopedId, FeedScopedId> stopAreas
  ) {
    this.legRules = legRules;
    this.transferRules = stripWildcards(fareTransferRules);
    this.networksWithRules = findNetworksWithRules(legRules);
    this.fromAreasWithRules = findAreasWithRules(legRules, FareLegRule::fromAreaId);
    this.toAreasWithRules = findAreasWithRules(legRules, FareLegRule::toAreaId);
    this.stopAreas = stopAreas;
  }

  Set<FareProductMatch.Transfer> transfersFromPreviousLeg(
    ScheduledTransitLeg previous,
    ScheduledTransitLeg current
  ) {
    var previousLegRules = legRuleIds(previous);
    var currentLegRules = legRuleIds(current);
    return transferRules
      .stream()
      .filter(f -> previousLegRules.contains(f.fromLegGroup()))
      .filter(f -> currentLegRules.contains(f.toLegGroup()))
      .flatMap(f -> {
        var requiredProducts = findFareLegRule(f.fromLegGroup())
          .stream()
          .flatMap(fr -> fr.fareProducts().stream())
          .toList();
        return f
          .fareProducts()
          .stream()
          .map(transferProduct -> new FareProductMatch.Transfer(transferProduct, requiredProducts));
      })
      .collect(Collectors.toUnmodifiableSet());
  }

  Set<FareLegRule> legRules(ScheduledTransitLeg leg) {
    return this.legRules.stream().filter(r -> legMatchesRule(leg, r)).collect(Collectors.toSet());
  }

  boolean legMatchesRule(ScheduledTransitLeg leg, FareLegRule rule) {
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

  Set<FareProductMatch.ProductWithTransfer> productsWithTransfer(
    ScheduledTransitLeg leg,
    Optional<ScheduledTransitLeg> nextLeg
  ) {
    var legRules = legRules(leg);

    return legRules
      .stream()
      .map(rule -> {
        var transferRulesToNextLeg = transferRulesToNextLeg(nextLeg, rule);
        return new FareProductMatch.ProductWithTransfer(rule, transferRulesToNextLeg);
      })
      .collect(Collectors.toSet());
  }

  private List<FareTransferRule> transferRulesToNextLeg(
    Optional<ScheduledTransitLeg> nextLeg,
    FareLegRule rule
  ) {
    return transferRules
      .stream()
      .filter(t -> t.fromLegGroup().equals(rule.legGroupId()))
      .filter(t -> transferRuleMatchesNextLeg(nextLeg, t))
      .toList();
  }

  private Optional<FareLegRule> findFareLegRule(FeedScopedId id) {
    return legRules.stream().filter(r -> r.legGroupId().equals(id)).findFirst();
  }

  private static List<FareTransferRule> stripWildcards(Collection<FareTransferRule> rules) {
    return rules.stream().filter(FareLookupService::checkForWildcards).toList();
  }

  private Set<FeedScopedId> legRuleIds(ScheduledTransitLeg current) {
    return legRules(current)
      .stream()
      .map(FareLegRule::legGroupId)
      .filter(Objects::nonNull)
      .collect(Collectors.toUnmodifiableSet());
  }

  private static boolean checkForWildcards(FareTransferRule t) {
    if (t.containsWildCard()) {
      LOG.warn(
        "Transfer rule {} contains a wildcard leg group reference. These are not supported yet.",
        t
      );
      return false;
    } else {
      return true;
    }
  }

  boolean transferRuleMatchesNextLeg(Optional<ScheduledTransitLeg> nextLeg, FareTransferRule t) {
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
  public boolean matchesNetworkId(ScheduledTransitLeg leg, FareLegRule rule) {
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
}
