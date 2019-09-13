package org.opentripplanner.routing.impl;

import org.opentripplanner.routing.services.FareService;

public class DutchFareServiceFactory extends DefaultFareServiceFactory {
    @Override
    public FareService makeFareService() { 
        return new DutchFareServiceImpl(regularFareRules.values());
    }
}
