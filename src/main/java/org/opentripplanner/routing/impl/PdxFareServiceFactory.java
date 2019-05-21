package org.opentripplanner.routing.impl;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.services.FareServiceFactory;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;

/**
 * Portland fare service factory requires that all standard GTFS fare attributes and rules are filled.
 *
 * TODO: Do we need to add other fare types (e.g., senior, children, etc.)?
 */
public class PdxFareServiceFactory extends DefaultFareServiceFactory {

    protected Map<FeedScopedId, FareRuleSet> regularFareRules = new HashMap<>();

    public FareService makeFareService() {
        PdxFareServiceImpl fareService = new PdxFareServiceImpl(regularFareRules.values());
        return fareService;
    }

    @Override
    public void processGtfs(OtpTransitService transitService) {
        fillFareRules(null, transitService.getAllFareAttributes(), transitService.getAllFareRules(), regularFareRules);
    }

    @Override
    public void configure(JsonNode config) {
    }
}
