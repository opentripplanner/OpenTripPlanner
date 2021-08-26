package org.opentripplanner.routing.impl;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.services.FareService;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;

/**
 * Portland (PDX) fare service factory requires that all standard GTFS fare attributes and rules are filled. I.e., TriMet fares
 * should be provided in GTFS as should C-TRAN fares (which currently do not exist in their GTFS feed as of 2019/06/19).
 *
 * The basic fare strategy in the Portland area is to allow transfers between operators and, if the route transferred to is run
 * by an operator with a higher fare, to upcharge the customer to meet the higher fare.
 *
 * TODO: Do we need to add other fare types (e.g., senior, children, etc.)?
 */
public class PdxFareServiceFactory extends DefaultFareServiceFactory {

    protected Map<FeedScopedId, FareRuleSet> regularFareRules = new HashMap<>();

    public FareService makeFareService() {
        PdxFareServiceImpl fareService = new PdxFareServiceImpl(regularFareRules.values());
        return fareService;
    }

    /**
     * This step ensures that the fares in the source GTFS data are accounted for correctly.
     */
    @Override
    public void processGtfs(OtpTransitService transitService) {
        fillFareRules(transitService.getAllFareAttributes(), transitService.getAllFareRules(), regularFareRules);
    }

    /**
     * There is no configuration code in DefaultFareServiceFactory. We override the superclass's method just in case it changes.
     */
    @Override
    public void configure(JsonNode config) {
        // No configuration at the moment.
    }
}
