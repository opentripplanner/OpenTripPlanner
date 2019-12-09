package org.opentripplanner.routing.impl;

import org.opentripplanner.annotation.Component;
import org.opentripplanner.annotation.ServiceType;
import org.opentripplanner.routing.services.FareService;

@Component(key = "san-francisco",type = ServiceType.ServiceFactory)
public class SFBayFareServiceFactory extends DefaultFareServiceFactory {
    @Override
    public FareService makeFareService() {
        return new SFBayFareServiceImpl(regularFareRules.values());
    }
}
