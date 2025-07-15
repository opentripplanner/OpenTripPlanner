package org.opentripplanner.ext.fares.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.ext.fares.model.FareRulesData;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.model.fare.ItineraryFare;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.routing.fares.FareServiceFactory;

/**
 * Create a FareServiceFactory which create a noop fare service. That is a fare service that does
 * nothing.
 */
public class NoopFareServiceFactory implements FareServiceFactory {

  @Override
  public FareService makeFareService() {
    return new NoopFareService();
  }

  @Override
  public void processGtfs(FareRulesData a, OtpTransitService b) {}

  @Override
  public void configure(JsonNode config) {}

  @Override
  public String toString() {
    return "NoopFareServiceFactory{}";
  }

  /**
   * A Noop {@link FareService} implementation. Must be serializable; Hence have a default
   * constructor.
   */
  private static class NoopFareService implements FareService {

    @Override
    public ItineraryFare calculateFares(Itinerary path) {
      return null;
    }
  }
}
