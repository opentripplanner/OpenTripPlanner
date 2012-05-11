package org.opentripplanner.routing.services;

public interface ChainedFareService extends FareService {
    void setNextService(FareService service);
}
