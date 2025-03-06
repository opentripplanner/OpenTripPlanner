package org.opentripplanner.ext.fares.impl;

import com.google.common.collect.Sets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.ext.fares.model.FareAttribute;
import org.opentripplanner.ext.fares.model.FareRuleSet;
import org.opentripplanner.ext.fares.model.RouteOriginDestination;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.transit.model.basic.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This fare service module handles single feed HSL ticket pricing logic.
 */

public class HSLFareService extends DefaultFareService {

  private static final Logger LOG = LoggerFactory.getLogger(HSLFareService.class);
  // this is not Float.MAX_VALUE to avoid overflow which would then make debugging harder
  public static final Money MAX_PRICE = Money.euros(999999f);

  @Override
  protected boolean shouldCombineInterlinedLegs(
    ScheduledTransitLeg previousLeg,
    ScheduledTransitLeg currentLeg
  ) {
    return true;
  }

  @Override
  protected Optional<FareAndId> getBestFareAndId(
    FareType fareType,
    List<Leg> legs,
    Collection<FareRuleSet> fareRules
  ) {
    Set<String> zones = new HashSet<>();
    ZonedDateTime startTime = legs.get(0).getStartTime();
    ZonedDateTime lastRideStartTime = startTime;

    Money specialRouteFare = MAX_PRICE;
    FareAttribute specialFareAttribute = null;

    String agency = null;
    boolean singleAgency = true;

    // Do not consider fares for legs that do not have fare rules in the same feed
    Set<String> fareRuleFeedIds = fareRules
      .stream()
      .map(fr -> fr.getFareAttribute().getId().getFeedId())
      .collect(Collectors.toSet());
    Set<String> legFeedIds = legs
      .stream()
      .map(leg -> leg.getAgency().getId().getFeedId())
      .collect(Collectors.toSet());
    if (!Sets.difference(legFeedIds, fareRuleFeedIds).isEmpty()) {
      return Optional.empty();
    }

    for (Leg leg : legs) {
      lastRideStartTime = leg.getStartTime();
      if (agency == null) {
        agency = leg.getAgency().getId().getId().toString();
      } else if (agency != leg.getAgency().getId().getId().toString()) {
        singleAgency = false;
      }

      /* HSL specific logic: all exception routes start and end from the defined zone set,
               but visit temporarily (maybe 1 stop only) an 'external' zone */
      Money bestSpecialFare = MAX_PRICE;

      Set<String> ruleZones = null;
      for (FareRuleSet ruleSet : fareRules) {
        if (
          ruleSet.hasAgencyDefined() &&
          leg.getAgency().getId().getId() != ruleSet.getAgency().getId()
        ) {
          continue;
        }
        RouteOriginDestination routeOriginDestination = new RouteOriginDestination(
          leg.getRoute().getId().toString(),
          leg.getFrom().stop.getFirstZoneAsString(),
          leg.getTo().stop.getFirstZoneAsString()
        );
        boolean isSpecialRoute = false;

        if (
          !ruleSet.getRouteOriginDestinations().isEmpty() &&
          ruleSet
            .getRouteOriginDestinations()
            .toString()
            .indexOf(routeOriginDestination.toString()) !=
          -1
        ) {
          isSpecialRoute = true;
        }
        if (
          isSpecialRoute ||
          (ruleSet.getRoutes().contains(leg.getRoute().getId()) &&
            ruleSet.getContains().contains(leg.getFrom().stop.getFirstZoneAsString()) &&
            ruleSet.getContains().contains(leg.getTo().stop.getFirstZoneAsString()))
        ) {
          // check validity of this special rule and that it is the cheapest applicable one
          FareAttribute attribute = ruleSet.getFareAttribute();
          if (
            !attribute.isTransferDurationSet() ||
            Duration.between(lastRideStartTime, startTime).getSeconds() <
            attribute.getTransferDuration()
          ) {
            Money newFare = attribute.getPrice();
            if (newFare.lessThan(bestSpecialFare)) {
              bestSpecialFare = newFare;
              ruleZones = ruleSet.getContains();
              if (isSpecialRoute) {
                specialRouteFare = bestSpecialFare;
                specialFareAttribute = attribute;
              }
            }
          }
        }
      }

      if (ruleZones != null) { // the special case
        // evaluate boolean ride.zones AND rule.zones
        Set<String> zoneIntersection = new HashSet<String>(
          leg.getFareZones().stream().map(z -> z.getId().getId()).toList()
        );
        zoneIntersection.retainAll(ruleZones); // don't add temporarily visited zones
        zones.addAll(zoneIntersection);
      } else {
        zones.addAll(leg.getFareZones().stream().map(z -> z.getId().getId()).toList());
      }
    }

    FareAttribute bestAttribute = null;
    Money bestFare = MAX_PRICE;
    long tripTime = Duration.between(startTime, lastRideStartTime).getSeconds();

    if (zones.size() > 0) {
      // find the best fare that matches this set of rides
      for (FareRuleSet ruleSet : fareRules) {
        // make sure the rule is applicable by agency requirements
        if (
          ruleSet.hasAgencyDefined() && (!singleAgency || agency != ruleSet.getAgency().getId())
        ) {
          continue;
        }
        /* another HSL specific change: We do not set rules for every possible zone combination,
                but for the largest zone set allowed for a certain ticket type.
                This way we need only a few rules instead of hundreds of rules. Good for speed!
                */
        if (ruleSet.getContains().containsAll(zones)) { // contains, not equals !!
          FareAttribute attribute = ruleSet.getFareAttribute();
          // transfers are evaluated at boarding time
          if (attribute.isTransferDurationSet()) {
            if (tripTime > attribute.getTransferDuration()) {
              LOG.debug(
                "transfer time exceeded; {} > {} in fare {}",
                tripTime,
                attribute.getTransferDuration(),
                attribute.getId()
              );
              continue;
            } else {
              LOG.debug(
                "transfer time OK; {} < {} in fare {}",
                tripTime,
                attribute.getTransferDuration(),
                attribute.getId()
              );
            }
          }
          Money newFare = attribute.getPrice();
          if (
            newFare.lessThan(bestFare) ||
            (newFare.equals(bestFare) && ruleSet.getContains().equals(zones))
          ) {
            bestAttribute = attribute;
            bestFare = newFare;
          }
        }
      }
    } else if (!specialRouteFare.equals(MAX_PRICE) && specialFareAttribute != null) {
      bestFare = specialRouteFare;
      bestAttribute = specialFareAttribute;
    }
    LOG.debug("HSL {} best for {}", bestAttribute, legs);
    final Money finalBestFare = bestFare;
    return Optional.ofNullable(bestAttribute).map(attribute ->
      new FareAndId(finalBestFare, attribute.getId())
    );
  }
}
