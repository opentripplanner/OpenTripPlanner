package org.opentripplanner.routing.fares.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.model.FareAttribute;
import org.opentripplanner.model.FareRule;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.model.Route;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.routing.fares.FareServiceFactory;
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

    protected Map<FeedScopedId, FareRuleSet> regularFareRules = new HashMap<>();

    @Override
    public FareService makeFareService() {
        DefaultFareServiceImpl fareService = new DefaultFareServiceImpl();
        fareService.addFareRules(FareType.regular, regularFareRules.values());
        return fareService;
    }

    @Override
    public void processGtfs(OtpTransitService transitService) {
        fillFareRules(transitService.getAllFareAttributes(), transitService.getAllFareRules(), regularFareRules);
    }

    protected void fillFareRules(Collection<FareAttribute> fareAttributes,
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

    @Override
    public String toString() { return this.getClass().getSimpleName(); }

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

        FareServiceFactory factory = createFactory(type);
        factory.configure(config);
        return factory;
    }

    private static FareServiceFactory createFactory(String type) {
        switch (type) {
        case "default":
            return new DefaultFareServiceFactory();
        case "off":
            return new NoopFareServiceFactory();
        case "composite:additive":
            return new MultipleFareServiceFactory.AddingMultipleFareServiceFactory();
        case "vehicle-rental-time-based":
        case "bike-rental-time-based": //TODO: deprecated, remove in next major version
            return new TimeBasedVehicleRentalFareServiceFactory();
        case "dutch":
            return new DutchFareServiceFactory();
        case "san-francisco":
            return new SFBayFareServiceFactory();
        case "new-york":
            return new NycFareServiceFactory();
        case "seattle":
            return new SeattleFareServiceFactory();
        default:
            throw new IllegalArgumentException(String.format("Unknown fare type: '%s'", type));
        }
    }
}
