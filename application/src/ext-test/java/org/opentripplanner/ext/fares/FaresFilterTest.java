package org.opentripplanner.ext.fares;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.fare.FareProductUse;
import org.opentripplanner.model.fare.ItineraryFare;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.Money;

public class FaresFilterTest implements PlanTestConstants {

  private final TimetableRepositoryForTest testModel = TimetableRepositoryForTest.of();

  @Test
  void shouldAddFare() {
    final int ID = 1;

    Itinerary i1 = newItinerary(A, 0)
      .walk(20, Place.forStop(testModel.stop("1:stop", 1d, 1d).build()))
      .bus(ID, 0, 50, B)
      .bus(ID, 52, 100, C)
      .build();

    assertEquals(ItineraryFare.empty(), i1.fare());

    var fares = new ItineraryFare();

    var leg = i1.legs().get(1);
    var fp = FareProduct.of(id("fp"), "fare product", Money.euros(10.00f)).build();
    fares.addFareProduct(leg, fp);

    var filter = new DecorateWithFare((FareService) itinerary -> fares);

    i1 = filter.decorate(i1);

    assertEquals(fares, i1.fare());

    var busLeg = i1.transitLeg(1);

    assertEquals(
      List.of(new FareProductUse("c1a04702-1fb6-32d4-ba02-483bf68111ed", fp)),
      busLeg.fareProducts()
    );
  }
}
