package org.opentripplanner.ext.fares.impl;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.ext.fares.model.FareAttribute;
import org.opentripplanner.ext.fares.model.FareRuleSet;
import org.opentripplanner.ext.fares.model.RouteOriginDestination;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.routing.core.FareType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This fare service module handles single feed HSL ticket pricing logic.
 */

public class HSLFareServiceImpl extends DefaultFareService {

  private static final Logger LOG = LoggerFactory.getLogger(HSLFareServiceImpl.class);

  @Override
  protected boolean shouldCombineInterlinedLegs(
    ScheduledTransitLeg previousLeg,
    ScheduledTransitLeg currentLeg
  ) {
    return true;
  }

  @Override
  protected FareAndId getBestFareAndId(
    FareType fareType,
    List<Leg> legs,
    Collection<FareRuleSet> fareRules
  ) {
    Set<String> zones = new HashSet<String>();
    ZonedDateTime startTime = legs.get(0).getStartTime();
    ZonedDateTime lastRideStartTime = startTime;

    float specialRouteFare = Float.POSITIVE_INFINITY;
    FareAttribute specialFareAttribute = null;

    String agency = null;
    boolean singleAgency = true;

    for (Leg leg : legs) {
      lastRideStartTime = leg.getStartTime();
      if (agency == null) {
        agency = leg.getAgency().getId().getId().toString();
      } else if (agency != leg.getAgency().getId().getId().toString()) {
        singleAgency = false;
      }
      /* HSL specific logic: all exception routes start and end from the defined zone set,
               but visit temporarily (maybe 1 stop only) an 'external' zone */
      float bestSpecialFare = Float.POSITIVE_INFINITY;
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
          (
            ruleSet.getRoutes().contains(leg.getRoute().getId()) &&
            ruleSet.getContains().contains(leg.getFrom().stop.getFirstZoneAsString()) &&
            ruleSet.getContains().contains(leg.getTo().stop.getFirstZoneAsString())
          )
        ) {
          // check validity of this special rule and that it is the cheapest applicable one
          FareAttribute attribute = ruleSet.getFareAttribute();
          if (
            !attribute.isTransferDurationSet() ||
            Duration.between(lastRideStartTime, startTime).getSeconds() <
            attribute.getTransferDuration()
          ) {
            float newFare = getFarePrice(attribute, fareType);
            if (newFare < bestSpecialFare) {
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
    float bestFare = Float.POSITIVE_INFINITY;
    long tripTime = Duration.between(startTime, lastRideStartTime).getSeconds();

    if (zones.size() > 0) {
      // find the best fare that matches this set of rides
      for (FareRuleSet ruleSet : fareRules) {
        // make sure the rule is applicable by agency requirements
        if (
          ruleSet.hasAgencyDefined() &&
          (singleAgency == false || agency != ruleSet.getAgency().getId())
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
          float newFare = getFarePrice(attribute, fareType);
          if (newFare < bestFare) {
            bestAttribute = attribute;
            bestFare = newFare;
          }
        }
      }
    } else if (specialRouteFare != Float.POSITIVE_INFINITY && specialFareAttribute != null) {
      bestFare = specialRouteFare;
      bestAttribute = specialFareAttribute;
    }
    LOG.debug("HSL {} best for {}", bestAttribute, legs);
    return new FareAndId(bestFare, bestAttribute == null ? null : bestAttribute.getId());
  }
}
