package org.opentripplanner.ext.fares.impl.gtfs;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.opentripplanner.utils.collection.ListUtils.partitionIntoOverlappingPairs;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
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
import org.opentripplanner.utils.collection.SetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FareLookupService implements Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(FareLookupService.class);
  private final List<FareLegRule> legRules;
  private final List<FareTransferRule> transferRules;
  private final SetMultimap<FeedScopedId, FeedScopedId> stopAreas;
  private final Set<FeedScopedId> networksWithRules;
  private final Set<FeedScopedId> fromAreasWithRules;
  private final Set<FeedScopedId> toAreasWithRules;
  private List<FareLegRule> fromRules;

  FareLookupService(
    List<FareLegRule> legRules,
    List<FareTransferRule> fareTransferRules,
    Multimap<FeedScopedId, FeedScopedId> stopAreas
  ) {
    this.legRules = List.copyOf(legRules);
    this.transferRules = stripWildcards(fareTransferRules);
    this.networksWithRules = findNetworksWithRules(legRules);
    this.fromAreasWithRules = findAreasWithRules(legRules, FareLegRule::fromAreaId);
    this.toAreasWithRules = findAreasWithRules(legRules, FareLegRule::toAreaId);
    this.stopAreas = ImmutableSetMultimap.copyOf(stopAreas);
  }

  /**
   * Returns true if the service contains no data at all.
   */
  boolean isEmpty() {
    return legRules.isEmpty() && transferRules.isEmpty();
  }

  /**
   * Returns the fare leg rules for a specific leg.
   */
  Set<FareLegRule> legRules(ScheduledTransitLeg leg) {
    return this.legRules.stream().filter(r -> legMatchesRule(leg, r)).collect(Collectors.toSet());
  }

  /**
   * Find those fare products that match all legs through an unlimited transfer.
   */
  Set<FareProduct> findTransfersMatchingAllLegs(List<ScheduledTransitLeg> legs) {
    return this.transferRules.stream()
      .filter(FareTransferRule::unlimitedTransfers)
      .filter(FareTransferRule::isFree)
      .flatMap(r -> findTransferMatches(r, legs).stream())
      .filter(transferMatch -> appliesToAllLegs(legs, transferMatch))
      .flatMap(transferRule -> transferRule.fromLegRule().fareProducts().stream())
      .collect(Collectors.toUnmodifiableSet());
  }

  private boolean appliesToAllLegs(List<ScheduledTransitLeg> legs, TransferMatch transferMatch) {
    return partitionIntoOverlappingPairs(legs)
      .stream()
      .allMatch(
        pair ->
          legMatchesRule(pair.first(), transferMatch.fromLegRule()) &&
          legMatchesRule(pair.second(), transferMatch.toLegRule())
      );
  }

  /**
   * Find fare offers for a specific pair of legs.
   */
  Set<FareOffer> findTransferOffersForSubLegs(
    ScheduledTransitLeg head,
    List<ScheduledTransitLeg> tail
  ) {
    Set<TransferMatch> rules =
      this.transferRules.stream()
        .flatMap(r -> {
          fromRules = findFareLegRule(r.fromLegGroup());
          var toRules = findFareLegRule(r.toLegGroup());
          if (fromRules.isEmpty() || toRules.isEmpty()) {
            return Stream.of();
          } else {
            var possibleTransfers = findPossibleTransfers(head, tail, r, fromRules, toRules);
            return SetUtils.intersection(possibleTransfers).stream();
          }
        })
        .collect(Collectors.toSet());

    Multimap<FareProduct, FareProduct> dependencies = HashMultimap.create();

    rules.forEach(transfer ->
      transfer
        .transferRule()
        .fareProducts()
        .forEach(p -> dependencies.putAll(p, transfer.fromLegRule().fareProducts()))
    );

    return dependencies
      .keySet()
      .stream()
      .map(product -> FareOffer.of(head.startTime(), product, dependencies.get(product)))
      .collect(Collectors.toSet());
  }

  boolean hasFreeTransfer(ScheduledTransitLeg head, List<ScheduledTransitLeg> tail) {
    return this.transferRules.stream()
      .anyMatch(r -> {
        var fromRules = findFareLegRule(r.fromLegGroup());
        var toRules = findFareLegRule(r.toLegGroup());
        if (fromRules.isEmpty() || toRules.isEmpty()) {
          return false;
        } else {
          return tail
            .stream()
            .allMatch(to ->
              findTransferMatches(head, to, r, fromRules, toRules).anyMatch(t ->
                t.transferRule().isFree()
              )
            );
        }
      });
  }

  private Set<Set<TransferMatch>> findPossibleTransfers(
    ScheduledTransitLeg head,
    List<ScheduledTransitLeg> tail,
    FareTransferRule r,
    List<FareLegRule> fromRules,
    List<FareLegRule> toRules
  ) {
    return tail
      .stream()
      .map(to ->
        findTransferMatches(head, to, r, fromRules, toRules).collect(Collectors.toUnmodifiableSet())
      )
      .collect(Collectors.toUnmodifiableSet());
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

  private List<TransferMatch> findTransferMatches(
    FareTransferRule transferRule,
    List<ScheduledTransitLeg> transitLegs
  ) {
    var pairs = partitionIntoOverlappingPairs(transitLegs);
    var fromRules = findFareLegRule(transferRule.fromLegGroup());
    var toRules = findFareLegRule(transferRule.toLegGroup());

    // no need to compute transfers if there is only a single leg or the rules cannot be found
    if (pairs.isEmpty() || fromRules.isEmpty() || toRules.isEmpty()) {
      return List.of();
    } else {
      return pairs
        .stream()
        .flatMap(pair -> {
          var from = pair.first();
          var to = pair.second();
          var matchingFrom = fromRules.stream().filter(rule -> legMatchesRule(from, rule)).toList();
          var matchingTo = toRules.stream().filter(rule -> legMatchesRule(to, rule)).toList();

          return matchingFrom
            .stream()
            .flatMap(fromR ->
              matchingTo.stream().map(toR -> new TransferMatch(transferRule, fromR, toR))
            );
        })
        .toList();
    }
  }

  private boolean legMatchesRule(ScheduledTransitLeg leg, FareLegRule rule) {
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
  private boolean matchesNetworkId(ScheduledTransitLeg leg, FareLegRule rule) {
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
}
