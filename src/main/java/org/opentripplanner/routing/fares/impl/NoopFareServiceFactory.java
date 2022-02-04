package org.opentripplanner.routing.fares.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.routing.fares.FareServiceFactory;


/**
 * Create a FareServiceFactory which create a noop fare service. That is a fare service
 * that does nothing.
 */
class NoopFareServiceFactory implements FareServiceFactory {
    @Override
    public FareService makeFareService() {
        return new NoopFareService();
    }

    @Override
    public void processGtfs(OtpTransitService transitService) { }

    @Override
    public void configure(JsonNode config) { }

    @Override
    public String toString() {
        return "NoopFareServiceFactory{}";
    }

    /**
     * A Noop {@link FareService} implementation. Must be serializable; Hence
     * have a default constructor.
     */
    private static class NoopFareService implements FareService {
        private static final Long serialVersionUID = 1L;
        @Override
        public Fare getCost(Itinerary path) {
            return null;
        }
    }
}
