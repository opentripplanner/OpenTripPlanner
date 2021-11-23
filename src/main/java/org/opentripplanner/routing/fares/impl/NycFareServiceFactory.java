package org.opentripplanner.routing.fares.impl;

import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.routing.fares.FareServiceFactory;

import com.fasterxml.jackson.databind.JsonNode;

public class NycFareServiceFactory implements FareServiceFactory {

    public FareService makeFareService() {
        return new NycFareServiceImpl();
    }

    @Override
    public void processGtfs(OtpTransitService transitService) {
    }

    @Override
    public void configure(JsonNode config) {
    }
}
