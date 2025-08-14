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

import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.fare.ItineraryFare;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.transit.model.basic.Money;

class ItineraryFareTest {

  private static final ZonedDateTime ANY_ZDT = ZonedDateTime.parse("2025-06-24T12:16:09Z");

  @Test
  void legProduct() {
    Itinerary i1 = newItinerary(A, T11_00)
      .walk(20, B)
      .bus(122, T11_01, T11_15, C)
      .rail(439, T11_30, T11_50, D)
      .build();

    var busLeg = i1.transitLeg(1);
    var railLeg = i1.transitLeg(2);

    var fares = new ItineraryFare();

    var busTicket = fareOffer("bus");
    var railTicketA = fareOffer("rail-a");
    var railTicketB = fareOffer("rail-b");

    fares.addFareProduct(busLeg, busTicket);
    fares.addFareProduct(railLeg, railTicketA);
    fares.addFareProduct(railLeg, railTicketB);

    assertEquals(List.of(busTicket), fares.getLegProducts().get(busLeg));

    assertEquals(List.of(railTicketA, railTicketB), fares.getLegProducts().get(railLeg));
  }

  @Test
  void empty() {
    assertTrue(ItineraryFare.empty().isEmpty());
  }

  private static FareOffer fareOffer(String id) {
    return FareOffer.of(ANY_ZDT, FareProduct.of(id(id), id, Money.euros(10)).build());
  }
}
