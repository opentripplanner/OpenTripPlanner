package org.opentripplanner.ext.stopconsolidation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.ext.stopconsolidation.TestStopConsolidationModel.STOP_C;
import static org.opentripplanner.ext.stopconsolidation.TestStopConsolidationModel.STOP_D;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_05;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_12;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.stopconsolidation.internal.DefaultStopConsolidationRepository;
import org.opentripplanner.ext.stopconsolidation.internal.DefaultStopConsolidationService;
import org.opentripplanner.ext.stopconsolidation.model.ConsolidatedStopGroup;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.fare.FareProductUse;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TestItineraryBuilder;
import org.opentripplanner.transit.model.basic.Money;

class DecorateConsolidatedStopNamesTest {

  private static final FareProduct fp = new FareProduct(
    id("fp"),
    "fare product",
    Money.euros(10.00f),
    null,
    null,
    null
  );
  private static final List<FareProductUse> fpu = List.of(
    new FareProductUse("c1a04702-1fb6-32d4-ba02-483bf68111ed", fp)
  );

  @Test
  void changeNames() {
    var transitModel = TestStopConsolidationModel.buildTransitModel();

    var groups = List.of(new ConsolidatedStopGroup(STOP_C.getId(), List.of(STOP_D.getId())));
    var repo = new DefaultStopConsolidationRepository();
    repo.addGroups(groups);

    var service = new DefaultStopConsolidationService(repo, transitModel);
    var filter = new DecorateConsolidatedStopNames(service);

    var itinerary = TestItineraryBuilder
      .newItinerary(Place.forStop(STOP_C))
      .bus(TestStopConsolidationModel.ROUTE, 1, T11_05, T11_12, Place.forStop(STOP_C))
      .bus(1, T11_05, T11_12, PlanTestConstants.E)
      .bus(1, T11_05, T11_12, PlanTestConstants.F)
      .build();

    itinerary.getLegs().getFirst().setFareProducts(fpu);

    filter.decorate(itinerary);

    var updatedLeg = itinerary.getLegs().getFirst();
    assertEquals(STOP_C.getName(), updatedLeg.getFrom().name);
    assertEquals(STOP_D.getName(), updatedLeg.getTo().name);

    // Check that the fares were carried over
    assertEquals(fpu, updatedLeg.fareProducts());
  }
}
