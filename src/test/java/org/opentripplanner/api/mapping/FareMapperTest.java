package org.opentripplanner.api.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.routing.core.ItineraryFares;
import org.opentripplanner.transit.model.basic.Money;

class FareMapperTest implements PlanTestConstants {

  @Test
  public void emptyDetails() {
    var fare = new ItineraryFares();
    fare.addFare(FareType.regular, Money.usDollars(500));

    Itinerary itinerary = newItinerary(A, 30).bus(1, 30, 60, B).bus(2, 90, 120, C).build();

    itinerary.setFare(fare);

    var mapper = new FareMapper(Locale.US);
    var apiFare = mapper.mapFare(itinerary);

    var apiMoney = apiFare.fare().get(FareType.regular.name());
    assertEquals(500, apiMoney.cents());
    assertEquals("USD", apiMoney.currency().currency());
    assertEquals(List.of(), apiFare.details().get(FareType.regular.name()));
  }
}
