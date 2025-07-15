package org.opentripplanner.ext.fares.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.ext.fares.model.FareAttribute;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareRule;
import org.opentripplanner.ext.fares.model.FareRuleSet;
import org.opentripplanner.ext.fares.model.FareRulesData;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.routing.fares.FareServiceFactory;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the default GTFS fare rules as described in http://groups.google.com/group/gtfs-changes/msg/4f81b826cb732f3b
 * as well as Fares V2.
 *
 */
public class DefaultFareServiceFactory implements FareServiceFactory {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultFareServiceFactory.class);

  protected Map<FeedScopedId, FareRuleSet> regularFareRules = new HashMap<>();

  private final List<FareLegRule> fareLegRules = new ArrayList<>();
  private final List<FareTransferRule> fareTransferRules = new ArrayList<>();

  // mapping the stop ids to area ids. one stop can be in several areas.
  private final Multimap<FeedScopedId, FeedScopedId> stopAreas = ArrayListMultimap.create();

  @Override
  public FareService makeFareService() {
    DefaultFareService fareService = new DefaultFareService();
    fareService.addFareRules(FareType.regular, regularFareRules.values());

    var faresV2Service = new GtfsFaresV2Service(fareLegRules, fareTransferRules, stopAreas);
    return new GtfsFaresService(fareService, faresV2Service);
  }

  @Override
  public void processGtfs(FareRulesData fareRulesData, OtpTransitService transitService) {
    fillFareRules(fareRulesData.fareAttributes(), fareRulesData.fareRules(), regularFareRules);
    this.fareLegRules.addAll(fareRulesData.fareLegRules());
    this.fareTransferRules.addAll(fareRulesData.fareTransferRules());
  }

  public void configure(JsonNode config) {
    // No configuration for the moment
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  protected void fillFareRules(
    Collection<FareAttribute> fareAttributes,
    Collection<FareRule> fareRules,
    Map<FeedScopedId, FareRuleSet> fareRuleSet
  ) {
    /*
     * Create an empty FareRuleSet for each FareAttribute, as some FareAttribute may have no
     * rules attached to them.
     */
    for (FareAttribute fare : fareAttributes) {
      FeedScopedId id = fare.getId();
      FareRuleSet fareRule = fareRuleSet.get(id);
      if (fareRule == null) {
        fareRule = new FareRuleSet(fare);
        fareRuleSet.put(id, fareRule);
      }
    }

    /*
     * For each fare rule, add it to the FareRuleSet of the fare.
     */
    for (FareRule rule : fareRules) {
      FareAttribute fare = rule.getFare();
      FeedScopedId id = fare.getId();
      FareRuleSet fareRule = fareRuleSet.get(id);
      if (fareRule == null) {
        // Should never happen by design
        LOG.error("Nonexistent fare ID in fare rule: " + id);
        continue;
      }
      String contains = rule.getContainsId();
      if (contains != null) {
        fareRule.addContains(contains);
      }
      String origin = rule.getOriginId();
      String destination = rule.getDestinationId();
      if (origin != null || destination != null) {
        fareRule.addOriginDestination(origin, destination);
      }
      Route route = rule.getRoute();
      if (route != null) {
        FeedScopedId routeId = route.getId();
        fareRule.addRoute(routeId);
      }
    }
  }
}
