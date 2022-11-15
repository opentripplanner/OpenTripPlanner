package org.opentripplanner.ext.fares;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.routing.core.ItineraryFares;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.Money;

public class FaresFilterTest implements PlanTestConstants {

  @Test
  public void shouldAddFare() {
    final int ID = 1;

    Itinerary i1 = newItinerary(A, 0)
      .walk(20, Place.forStop(TransitModelForTest.stopForTest("1:stop", 1d, 1d)))
      .bus(ID, 0, 50, B)
      .bus(ID, 52, 100, C)
      .build();

    List<Itinerary> input = List.of(i1, i1, i1);

    input.forEach(i -> assertEquals(ItineraryFares.empty(), i.getFares()));

    var twoEighty = new ItineraryFares();
    twoEighty.addFare(FareType.regular, Money.euros(280));

    var filter = new FaresFilter((FareService) itinerary -> twoEighty);
    var filtered = filter.filter(input);

    filtered.forEach(i -> {
      assertEquals(twoEighty, i.getFares());
    });
  }
}
