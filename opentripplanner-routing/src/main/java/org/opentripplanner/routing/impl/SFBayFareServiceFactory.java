package org.opentripplanner.routing.impl;

import org.opentripplanner.routing.services.FareService;

public class SFBayFareServiceFactory extends DefaultFareServiceFactory {
    @Override
    public FareService makeFareService() { 
        return new SFBayFareServiceImpl(fareRules, fareAttributes);
    }
}
