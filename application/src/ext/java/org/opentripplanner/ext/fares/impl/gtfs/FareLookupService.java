package org.opentripplanner.ext.fares.impl.gtfs;

import static org.opentripplanner.utils.collection.ListUtils.partitionIntoOverlappingPairs;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.plan.TransitLeg;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.collection.SetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FareLookupService implements Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(FareLookupService.class);
  private final List<FareLegRule> legRules;
  private final List<FareTransferRule> transferRules;
  private final AreaMatcher areaMatcher;
  private final NetworkMatcher networkMatcher;

  FareLookupService(
    List<FareLegRule> legRules,
    List<FareTransferRule> fareTransferRules,
    Multimap<FeedScopedId, FeedScopedId> stopAreas
  ) {
    this.legRules = List.copyOf(legRules);
    this.transferRules = stripWildcards(fareTransferRules);

    var rulePriorityMatcher = new RulePriorityMatcher(legRules);
    this.areaMatcher = new AreaMatcher(rulePriorityMatcher, legRules, stopAreas);
    this.networkMatcher = new NetworkMatcher(rulePriorityMatcher, legRules);
  }

  /**
   * Are there free transfers between the legs?
   */
  boolean hasFreeTransfers(List<TransitLeg> legs) {
    return !findTransfersMatchingAllLegs(legs).isEmpty();
  }

  /**
   * Returns true if the service contains no data at all.
   */
  boolean isEmpty() {
    return legRules.isEmpty() && transferRules.isEmpty();
  }

  /**
   * Returns the fare leg rules for a specific leg taking into account rule priorities, if they
   * exist.
   */
  Set<FareLegRule> legRules(TransitLeg leg) {
    var rules =
      this.legRules.stream()
        .filter(r -> legMatchesRule(leg, r))
        .collect(Collectors.toUnmodifiableSet());
    var containsPriorities = rules.stream().anyMatch(r -> r.priority().isPresent());
    if (containsPriorities) {
      return findHighestPriority(rules);
    } else {
      return rules;
    }
  }

  /**
   * Find those fare products that match all legs through an unlimited transfer.
   */
  Set<FareProduct> findTransfersMatchingAllLegs(List<TransitLeg> legs) {
    if (legs.size() < 2) {
      return Set.of();
    }
    return this.transferRules.stream()
      .filter(FareTransferRule::unlimitedTransfers)
      .filter(FareTransferRule::isFree)
      .filter(r -> TimeLimitEvaluator.withinTimeLimit(r, legs.getFirst(), legs.getLast()))
      .flatMap(r -> findTransferMatches(r, legs).stream())
      .filter(transferMatch -> appliesToAllLegs(legs, transferMatch))
      .flatMap(transferRule -> transferRule.fromLegRule().fareProducts().stream())
      .collect(Collectors.toUnmodifiableSet());
  }

  private boolean appliesToAllLegs(List<TransitLeg> legs, TransferMatch transferMatch) {
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
  Set<LegOffer> findTransferOffersForSubLegs(TransitLeg head, List<TransitLeg> tail) {
    Set<TransferMatch> transfers =
      this.transferRules.stream()
        .flatMap(r -> {
          var fromRules = findFareLegRule(r.fromLegGroup());
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

    transfers.forEach(transfer ->
      transfer
        .transferRule()
        .fareProducts()
        .forEach(p -> dependencies.putAll(p, transfer.fromLegRule().fareProducts()))
    );

    Set<LegOffer> dependentOffers = dependencies
      .keySet()
      .stream()
      .map(product ->
        LegOffer.of(FareOffer.of(head.startTime(), product, dependencies.get(product)))
      )
      .collect(Collectors.toSet());

    Set<LegOffer> freeTransferOffers = transfers
      .stream()
      .filter(TransferMatch::isFree)
      .flatMap(t ->
        t
          .fromLegRule()
          .fareProducts()
          .stream()
          .map(product ->
            LegOffer.of(
              FareOffer.of(head.startTime(), product, dependencies.get(product)),
              head,
              t.transferRule().timeLimit().orElse(null)
            )
          )
      )
      .collect(Collectors.toUnmodifiableSet());

    return SetUtils.combine(dependentOffers, freeTransferOffers);
  }

  private Set<Set<TransferMatch>> findPossibleTransfers(
    TransitLeg head,
    List<TransitLeg> tail,
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
    TransitLeg from,
    TransitLeg to,
    FareTransferRule r,
    List<FareLegRule> fromRules,
    List<FareLegRule> toRules
  ) {
    return fromRules
      .stream()
      .filter(match -> TimeLimitEvaluator.withinTimeLimit(r, from, to))
      .flatMap(fromRule -> toRules.stream().map(toRule -> new TransferMatch(r, fromRule, toRule)))
      .filter(
        match -> legMatchesRule(from, match.fromLegRule()) && legMatchesRule(to, match.toLegRule())
      );
  }

  private List<TransferMatch> findTransferMatches(
    FareTransferRule transferRule,
    List<TransitLeg> transitLegs
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

  private boolean legMatchesRule(TransitLeg leg, FareLegRule rule) {
    // make sure that you only get rules for the correct feed
    return (
      leg.agency().getId().getFeedId().equals(rule.feedId()) &&
      networkMatcher.matchesNetworkId(leg, rule) &&
      // apply only those fare leg rules which have the correct area ids
      // if area id is null, the rule applies to all legs UNLESS there is another rule that
      // covers this area
      areaMatcher.matchesFromArea(leg.from().stop, rule.fromAreaId()) &&
      areaMatcher.matchesToArea(leg.to().stop, rule.toAreaId()) &&
      DistanceMatcher.matchesDistance(leg, rule)
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

  /**
   * If a GTFS feed contains rule_priority values, then the highest priority rule is returned.
   *
   * @link <a href="https://gtfs.org/documentation/schedule/reference/#fare_leg_rulestxt">spec</a>
   */
  private static Set<FareLegRule> findHighestPriority(Set<FareLegRule> rules) {
    var maxPriority = rules.stream().mapToInt(r -> r.priority().orElse(0)).max().orElse(0);
    return rules
      .stream()
      .filter(r -> r.priority().orElse(0) == maxPriority)
      .collect(Collectors.toUnmodifiableSet());
  }
}
