package org.opentripplanner.routing.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.services.FareServiceFactory;


/**
 * Create a FareServiceFactory witch create a noop fare service. That is a fare service
 * that does nothing.
 */
class NoopFareServiceFactory implements FareServiceFactory {
    @Override
    public FareService makeFareService() {
        return (path, transitLayer) -> null;
    }
    @Override
    public void processGtfs(OtpTransitService transitService) { }

    @Override
    public void configure(JsonNode config) { }

    @Override
    public String toString() {
        return "NoopFareServiceFactory{}";
    }
}
