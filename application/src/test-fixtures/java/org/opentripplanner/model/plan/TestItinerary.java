package org.opentripplanner.model.plan;

import java.util.List;
import org.opentripplanner.framework.model.Cost;

public class TestItinerary {

  public static ItineraryBuilder of(Leg... legs) {
    return Itinerary.ofScheduledTransit(List.of(legs)).withGeneralizedCost(Cost.ZERO);
  }
}
