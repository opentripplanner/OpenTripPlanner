package org.opentripplanner.routing.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.fare.FareProductUse;
import org.opentripplanner.model.fare.ItineraryFares;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.transit.model.basic.Money;

class ItineraryFaresTest {

  @Test
  void legProduct() {
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

    assertEquals(
      List.of(new FareProductUse("606b5587-d460-3b2a-bf83-fa0bc03c24f3", busTicket)),
      fares.getLegProducts().get(busLeg)
    );

    assertEquals(
      List.of(
        new FareProductUse("5ac59bb6-56fa-31c9-9f2b-915797a22763", railTicketA),
        new FareProductUse("73f4c43f-b237-36d6-bc0a-2fc3aad98780", railTicketB)
      ),
      fares.getLegProducts().get(railLeg)
    );
  }

  @Test
  void empty() {
    assertTrue(ItineraryFares.empty().isEmpty());
  }

  private static FareProduct fareProduct(String id) {
    return new FareProduct(id(id), id, Money.euros(10), null, null, null);
  }
}
