package org.opentripplanner.ext.stopconsolidation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.ext.fares.impl.FareModelForTest.FARE_PRODUCT_USE;
import static org.opentripplanner.ext.stopconsolidation.TestStopConsolidationModel.STOP_C;
import static org.opentripplanner.ext.stopconsolidation.TestStopConsolidationModel.STOP_D;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_05;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_12;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.stopconsolidation.internal.DefaultStopConsolidationRepository;
import org.opentripplanner.ext.stopconsolidation.internal.DefaultStopConsolidationService;
import org.opentripplanner.ext.stopconsolidation.model.ConsolidatedStopGroup;
import org.opentripplanner.ext.stopconsolidation.model.ConsolidatedStopLeg;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TestItineraryBuilder;
import org.opentripplanner.model.plan.leg.ScheduledTransitLeg;
import org.opentripplanner.model.plan.leg.StreetLeg;

class DecorateConsolidatedStopNamesTest {

  private static final List<ConsolidatedStopGroup> GROUPS = List.of(
    new ConsolidatedStopGroup(STOP_C.getId(), List.of(STOP_D.getId()))
  );
  private static final Place PLACE_C = Place.forStop(STOP_C);

  @Test
  void changeNames() {
    var filter = defaultFilter();

    var itinerary = TestItineraryBuilder.newItinerary(PLACE_C)
      .bus(TestStopConsolidationModel.ROUTE, 1, T11_05, T11_12, PLACE_C)
      .bus(1, T11_05, T11_12, PlanTestConstants.E)
      .bus(1, T11_05, T11_12, PlanTestConstants.F)
      .build();

    var first = (ScheduledTransitLeg) itinerary.legs().getFirst();
    var withFp = first.copyOf().withFareProducts(List.of(FARE_PRODUCT_USE)).build();
    var legs = new ArrayList<>(itinerary.legs());
    legs.set(0, withFp);

    itinerary = itinerary.copyOf().withLegs(legs).build();

    itinerary = filter.decorate(itinerary);

    var updatedLeg = itinerary.legs().getFirst();
    assertEquals(STOP_C.getName(), updatedLeg.from().name);
    assertEquals(STOP_D.getName(), updatedLeg.to().name);

    // Check that the fares were carried over
    assertEquals(List.of(FARE_PRODUCT_USE), updatedLeg.fareProducts());
  }

  @Test
  void removeTransferAtConsolidatedStop() {
    final var filter = defaultFilter();

    var itinerary = TestItineraryBuilder.newItinerary(PLACE_C)
      .bus(TestStopConsolidationModel.ROUTE, 1, T11_05, T11_12, PLACE_C)
      .walk(1, PLACE_C)
      .bus(1, T11_05, T11_12, PlanTestConstants.F)
      .build();

    itinerary = filter.decorate(itinerary);

    var legs = itinerary.legs().stream().map(Leg::getClass).toList();
    assertEquals(List.of(ConsolidatedStopLeg.class, ScheduledTransitLeg.class), legs);
  }

  @Test
  void keepRegularTransfer() {
    final var filter = defaultFilter();

    var itinerary = TestItineraryBuilder.newItinerary(PLACE_C)
      .bus(TestStopConsolidationModel.ROUTE, 1, T11_05, T11_12, PLACE_C)
      .walk(1, PlanTestConstants.E)
      .bus(1, T11_05, T11_12, PlanTestConstants.F)
      .build();

    itinerary = filter.decorate(itinerary);

    var legs = itinerary.legs().stream().map(Leg::getClass).toList();
    assertEquals(
      List.of(ConsolidatedStopLeg.class, StreetLeg.class, ScheduledTransitLeg.class),
      legs
    );
  }

  private static DecorateConsolidatedStopNames defaultFilter() {
    var timetableRepository = TestStopConsolidationModel.buildTimetableRepository();

    var repo = new DefaultStopConsolidationRepository();
    repo.addGroups(GROUPS);

    var service = new DefaultStopConsolidationService(repo, timetableRepository);
    return new DecorateConsolidatedStopNames(service);
  }
}
