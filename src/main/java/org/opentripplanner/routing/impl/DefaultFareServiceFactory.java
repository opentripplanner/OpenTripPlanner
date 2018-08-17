package org.opentripplanner.routing.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.FareAttribute;
import org.opentripplanner.model.FareRule;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.routing.bike_rental.TimeBasedBikeRentalFareServiceFactory;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.fares.MultipleFareServiceFactory;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.services.FareServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Implements the default GTFS fare rules as described in
 * http://groups.google.com/group/gtfs-changes/msg/4f81b826cb732f3b
 *
 * @author novalis
 *
 */
public class DefaultFareServiceFactory implements FareServiceFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultFareServiceFactory.class);

    protected Map<FeedScopedId, FareRuleSet> regularFareRules = new HashMap<FeedScopedId, FareRuleSet>();

    public FareService makeFareService() {
        DefaultFareServiceImpl fareService = new DefaultFareServiceImpl();
        fareService.addFareRules(FareType.regular, regularFareRules.values());
        return fareService;
    }

    @Override
    public void processGtfs(OtpTransitService transitService) {
        fillFareRules(null, transitService.getAllFareAttributes(), transitService.getAllFareRules(), regularFareRules);
    }

    protected void fillFareRules(String agencyId, Collection<FareAttribute> fareAttributes,
            Collection<FareRule> fareRules, Map<FeedScopedId, FareRuleSet> fareRuleSet) {
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
                if (agencyId != null) {
                    // TODO With the new GTFS lib, use fareAttribute.agency_id directly
                    fareRule.setAgency(agencyId);
                }
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
                LOG.error("Inexistant fare ID in fare rule: " + id);
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

    public void configure(JsonNode config) {
        // No configuration for the moment
    }

    /**
     * Build a specific FareServiceFactory given the config node, or fallback to the default if none
     * specified.
     *
     * Accept different formats. Examples:
     *
     * <pre>
     * { fares : "seattle" }
     * --------------------------
     * { fares : {} } // Fallback to default
     * --------------------------
     * { fares : {
     *       type : "foobar",
     *       param1 : 42
     * } }
     * --------------------------
     * { fares : {
     *       combinationStrategy : "additive",
     *       fares : [
     *           "seattle",
     *           { type : "foobar", ... }
     *       ]
     * } }
     * </pre>
     */
    public static FareServiceFactory fromConfig(JsonNode config) {
        String type = null;
        if (config == null) {
            /* Empty block, fallback to default */
            type = null;
        } else if (config.isTextual()) {
            /* Simplest form: { fares : "seattle" } */
            type = config.asText();
        } else if (config.has("combinationStrategy")) {
            /* Composite */
            String combinationStrategy = config.path("combinationStrategy").asText();
            switch (combinationStrategy) {
            case "additive":
                break;
            default:
                throw new IllegalArgumentException("Unknown fare combinationStrategy: "
                        + combinationStrategy);
            }
            type = "composite:" + combinationStrategy;
        } else if (config.has("type")) {
            /* Fare with a type: { fares : { type : "foobar", param1 : 42 } } */
            type = config.path("type").asText(null);
        }

        if (type == null) {
            type = "default";
        }

        FareServiceFactory retval;
        switch (type) {
        case "default":
            retval = new DefaultFareServiceFactory();
            break;
        case "composite:additive":
            retval = new MultipleFareServiceFactory.AddingMultipleFareServiceFactory();
            break;
        case "bike-rental-time-based":
            retval = new TimeBasedBikeRentalFareServiceFactory();
            break;
        case "dutch":
            retval = new DutchFareServiceFactory();
            break;
        case "san-francisco":
            retval = new SFBayFareServiceFactory();
            break;
        case "new-york":
            retval = new NycFareServiceFactory();
            break;
        case "seattle":
            retval = new SeattleFareServiceFactory();
            break;
        default:
            throw new IllegalArgumentException(String.format("Unknown fare type: '%s'", type));
        }
        retval.configure(config);
        return retval;
    }

    public String toString() { return this.getClass().getSimpleName(); }
}
