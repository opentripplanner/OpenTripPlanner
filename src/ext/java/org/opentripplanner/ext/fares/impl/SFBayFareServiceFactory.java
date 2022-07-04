package org.opentripplanner.ext.fares.impl;

import org.opentripplanner.routing.fares.FareService;

public class SFBayFareServiceFactory extends DefaultFareServiceFactory {

  @Override
  public FareService makeFareService() {
    return new SFBayFareServiceImpl(regularFareRules.values());
  }
}
