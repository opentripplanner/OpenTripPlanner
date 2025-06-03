package org.opentripplanner.ext.fares.impl.gtfs;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.ext.fares.model.FareDistance;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.plan.leg.ScheduledTransitLeg;
import org.opentripplanner.transit.model.basic.Distance;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.utils.collection.ListUtils;
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

  boolean isEmpty() {
    return legRules.isEmpty() && transferRules.isEmpty();
  }

  Set<FareLegRule> legRules(ScheduledTransitLeg leg) {
    return this.legRules.stream().filter(r -> legMatchesRule(leg, r)).collect(Collectors.toSet());
  }

  Set<TransferMatch> transfersMatchingAllLegs(List<ScheduledTransitLeg> legs) {
    return this.transferRules.stream()
      .filter(FareTransferRule::unlimitedTransfers)
      .filter(FareTransferRule::isFree)
      .map(r -> findTransferMatch(r, legs))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(Collectors.toSet());
  }

  Set<FareOffer> findTransfersForPair(ScheduledTransitLeg from, ScheduledTransitLeg to) {
    Set<TransferMatch> rules =
      this.transferRules.stream()
        .flatMap(r -> {
          var fromRules = findFareLegRule(r.fromLegGroup());
          var toRules = findFareLegRule(r.toLegGroup());
          if (fromRules.isEmpty() || toRules.isEmpty()) {
            return Stream.of();
          } else {
            return findTransferMatches(from, to, r, fromRules, toRules);
          }
        })
        .collect(Collectors.toSet());

    Multimap<FareProduct, FareProduct> multiMap = HashMultimap.create();

    rules.forEach(transfer ->
      transfer
        .transferRule()
        .fareProducts()
        .forEach(p -> multiMap.putAll(p, transfer.fromLegRule().fareProducts()))
    );

    return multiMap
      .keySet()
      .stream()
      .map(product -> FareOffer.of(product, multiMap.get(product)))
      .collect(Collectors.toSet());
  }

  private Stream<TransferMatch> findTransferMatches(
    ScheduledTransitLeg from,
    ScheduledTransitLeg to,
    FareTransferRule r,
    List<FareLegRule> fromRules,
    List<FareLegRule> toRules
  ) {
    return fromRules
      .stream()
      .flatMap(fromRule -> toRules.stream().map(toRule -> new TransferMatch(r, fromRule, toRule)))
      .filter(
        match -> legMatchesRule(from, match.fromLegRule()) && legMatchesRule(to, match.toLegRule())
      );
  }

  private Optional<TransferMatch> findTransferMatch(
    FareTransferRule transferRule,
    List<ScheduledTransitLeg> transitLegs
  ) {
    var pairs = ListUtils.partitionIntoOverlappingPairs(transitLegs);
    var fromRule = Optional.ofNullable(findFareLegRule(transferRule.fromLegGroup()).getFirst());
    var toRule = Optional.ofNullable(findFareLegRule(transferRule.toLegGroup()).getLast());

    // no need to compute transfers if there is only a single leg or the rules cannot be found
    if (pairs.isEmpty() || fromRule.isEmpty() || toRule.isEmpty()) {
      return Optional.empty();
    } else {
      var matches = pairs
        .stream()
        .allMatch(
          pair ->
            legMatchesRule(pair.first(), fromRule.get()) &&
            legMatchesRule(pair.second(), toRule.get())
        );
      if (matches) {
        return Optional.of(new TransferMatch(transferRule, fromRule.get(), toRule.get()));
      } else {
        return Optional.empty();
      }
    }
  }

  boolean legMatchesRule(ScheduledTransitLeg leg, FareLegRule rule) {
    // make sure that you only get rules for the correct feed
    return (
      leg.agency().getId().getFeedId().equals(rule.feedId()) &&
      matchesNetworkId(leg, rule) &&
      // apply only those fare leg rules which have the correct area ids
      // if area id is null, the rule applies to all legs UNLESS there is another rule that
      // covers this area
      matchesArea(leg.from().stop, rule.fromAreaId(), fromAreasWithRules) &&
      matchesArea(leg.to().stop, rule.toAreaId(), toAreasWithRules) &&
      matchesDistance(leg, rule)
    );
  }

  private List<FareLegRule> findFareLegRule(FeedScopedId id) {
    return legRules.stream().filter(r -> r.legGroupId().equals(id)).toList();
  }

  private static List<FareTransferRule> stripWildcards(Collection<FareTransferRule> rules) {
    return rules.stream().filter(FareLookupService::checkForWildcards).toList();
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
      .route()
      .getGroupsOfRoutes()
      .stream()
      .map(AbstractTransitEntity::getId)
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
      var numStops = leg.listIntermediateStops().size();
      return numStops >= min && max >= numStops;
    } else if (
      rule.fareDistance() instanceof FareDistance.LinearDistance(Distance min, Distance max)
    ) {
      var legDistance = leg.directDistanceMeters();

      return legDistance > min.toMeters() && legDistance < max.toMeters();
    } else return true;
  }
}
