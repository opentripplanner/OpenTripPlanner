package org.opentripplanner.routing.core;

import static org.opentripplanner.model.plan.PlanTestConstants.A;
import static org.opentripplanner.model.plan.PlanTestConstants.B;
import static org.opentripplanner.model.plan.PlanTestConstants.C;
import static org.opentripplanner.model.plan.PlanTestConstants.D;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_00;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_01;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_15;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_30;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_50;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.fares.model.FareProduct;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.transit.model.basic.Money;

class ItineraryFaresTest {

  @Test
  void indexedLegProducts() {
    Itinerary i1 = newItinerary(A, T11_00)
      .walk(20, B)
      .bus(122, T11_01, T11_15, C)
      .rail(439, T11_30, T11_50, D)
      .build();

    var busLeg = i1.getTransitLeg(1);
    var railLeg = i1.getTransitLeg(2);

    var fares = new ItineraryFares();

    var busTicket = fareProduct("bus");
    var railTicketA = fareProduct("rail-a");
    var railTicketB = fareProduct("rail-b");

    fares.addFareProduct(busLeg, busTicket);
    fares.addFareProduct(railLeg, railTicketA);
    fares.addFareProduct(railLeg, railTicketB);
  }

  @Nonnull
  private static FareProduct fareProduct(String id) {
    return new FareProduct(id(id), id, Money.euros(1000), null, null, null);
  }
}
