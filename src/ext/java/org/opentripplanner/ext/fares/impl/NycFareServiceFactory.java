package org.opentripplanner.ext.fares.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.ext.fares.model.FareRulesData;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.routing.fares.FareServiceFactory;

public class NycFareServiceFactory implements FareServiceFactory {

  public FareService makeFareService() {
    return new NycFareServiceImpl();
  }

  @Override
  public void processGtfs(FareRulesData a, OtpTransitService b) {}

  @Override
  public void configure(JsonNode config) {}
}
