package org.opentripplanner.routing.fares.impl;

import org.opentripplanner.routing.fares.FareService;

public class DutchFareServiceFactory extends DefaultFareServiceFactory {
    @Override
    public FareService makeFareService() { 
        return new DutchFareServiceImpl(regularFareRules.values());
    }
}
