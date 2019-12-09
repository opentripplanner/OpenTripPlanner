package org.opentripplanner.routing.impl;

import org.opentripplanner.annotation.Component;
import org.opentripplanner.annotation.ServiceType;
import org.opentripplanner.routing.services.FareService;

@Component(key = "dutch",type = ServiceType.ServiceFactory)
public class DutchFareServiceFactory extends DefaultFareServiceFactory {
    @Override
    public FareService makeFareService() {
        return new DutchFareServiceImpl(regularFareRules.values());
    }
}
