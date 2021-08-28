package org.opentripplanner.routing.impl;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.services.FareService;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;

/**
 * The highest fare in free transfer window fare service factory requires that all standard GTFS fare attributes and
 * rules are filled. I.e., fares should be provided in GTFS.
 *
 * This fare service allows transfers between operators and, if the route transferred to is run by an operator with a
 * higher fare, the customer will be charged the higher fare. Also, the higher fare is used up until the end of the free
 * transfer window. The length of the free transfer window is configurable, but defaults to 2.5 hours.
 *
 * Additionally, there is an option to treat interlined transfers as actual transfers. This is merely a work-around for
 * transit agencies that choose to code their fares in a route-based fashion instead of a zone-based fashion.
 */
public class HighestFareInFreeTransferWindowFareServiceFactory extends DefaultFareServiceFactory {

    protected Map<FeedScopedId, FareRuleSet> regularFareRules = new HashMap<>();

    // default to 150 minutes to preserve compatibility with legacy pdx fares
    private int freeTransferWindowInMinutes = 150;
    private boolean analyzeInterlinedTransfers = false;

    public FareService makeFareService() {
        HighestFareInFreeTransferWindowFareServiceImpl fareService =
            new HighestFareInFreeTransferWindowFareServiceImpl(
                regularFareRules.values(),
                freeTransferWindowInMinutes,
                analyzeInterlinedTransfers
            );
        return fareService;
    }

    /**
     * This step ensures that the fares in the source GTFS data are accounted for correctly.
     */
    @Override
    public void processGtfs(OtpTransitService transitService) {
        fillFareRules(null, transitService.getAllFareAttributes(), transitService.getAllFareRules(), regularFareRules);
    }

    @Override
    public void configure(JsonNode config) {
        JsonNode freeTransferNode = config.path("freeTransferWindowInMinutes");
        if (freeTransferNode.isInt()) {
            freeTransferWindowInMinutes = freeTransferNode.asInt();
        }

        JsonNode analyzeInterlinedTransfersNode = config.path("analyzeInterlinedTransfers");
        if (analyzeInterlinedTransfersNode.isBoolean()) {
            analyzeInterlinedTransfers = analyzeInterlinedTransfersNode.asBoolean();
        }
    }
}
