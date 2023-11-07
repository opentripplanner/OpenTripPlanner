package org.opentripplanner.ext.fares;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.fare.FareProductUse;
import org.opentripplanner.model.fare.ItineraryFares;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.Money;

public class FaresFilterTest implements PlanTestConstants {

  private final TransitModelForTest testModel = TransitModelForTest.of();

  @Test
  void shouldAddFare() {
    final int ID = 1;

    Itinerary i1 = newItinerary(A, 0)
      .walk(20, Place.forStop(testModel.stop("1:stop", 1d, 1d).build()))
      .bus(ID, 0, 50, B)
      .bus(ID, 52, 100, C)
      .build();

    List<Itinerary> input = List.of(i1, i1, i1);

    input.forEach(i -> assertEquals(ItineraryFares.empty(), i.getFares()));

    var fares = new ItineraryFares();
    fares.addFare(FareType.regular, Money.euros(2.80f));

    var leg = i1.getLegs().get(1);
    var fp = new FareProduct(id("fp"), "fare product", Money.euros(10.00f), null, null, null);
    fares.addFareProduct(leg, fp);

    var filter = new FaresFilter((FareService) itinerary -> fares);
    var filtered = filter.filter(input);

    filtered.forEach(i -> assertEquals(fares, i.getFares()));

    var busLeg = filtered.get(0).getTransitLeg(1);

    assertEquals(
      List.of(new FareProductUse("c1a04702-1fb6-32d4-ba02-483bf68111ed", fp)),
      busLeg.fareProducts()
    );
  }
}
