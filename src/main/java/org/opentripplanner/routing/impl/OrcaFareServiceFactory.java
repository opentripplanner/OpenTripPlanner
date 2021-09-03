package org.opentripplanner.routing.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.services.FareService;

import java.util.HashMap;
import java.util.Map;

public class OrcaFareServiceFactory extends DefaultFareServiceFactory {
    protected Map<FeedScopedId, FareRuleSet> regularFareRules = new HashMap<>();

    @Override
    public FareService makeFareService() {
        return new OrcaFareServiceImpl(regularFareRules.values());
    }

    /**
     * This step ensures that the fares in the source GTFS data are accounted for correctly.
     */
    @Override
    public void processGtfs(OtpTransitService transitService) {
        fillFareRules(transitService.getAllFareAttributes(), transitService.getAllFareRules(), regularFareRules);
    }

    /**
     * There is no configuration code in DefaultFareServiceFactory. We override the super class's method just in case it changes.
     */
    @Override
    public void configure(JsonNode config) {
        // No configuration at the moment.
    }
}
