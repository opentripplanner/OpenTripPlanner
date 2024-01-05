package org.opentripplanner.routing.algorithm.filterchain.filters.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.BUS_ROUTE;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TimePeriod;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.TransitModel;

class TransitAlertFilterTest implements PlanTestConstants {

  private static final FeedScopedId ID = new FeedScopedId("FEED", "BUS");

  @Test
  void testFilter() {
    var transitAlertService = new TransitAlertServiceImpl(new TransitModel());
    transitAlertService.setAlerts(
      List.of(
        TransitAlert
          .of(ID)
          .addEntity(new EntitySelector.Route(BUS_ROUTE.getId()))
          .addTimePeriod(new TimePeriod(0, TimePeriod.OPEN_ENDED))
          .build()
      )
    );

    DecorateTransitAlert filter = new DecorateTransitAlert(transitAlertService, ignore -> null);

    // Expect filter to no fail on an empty list
    assertEquals(List.of(), filter.filter(List.of()));

    // Given a list with one itinerary
    List<Itinerary> list = List.of(
      newItinerary(A).bus(31, 0, 30, E).build(),
      newItinerary(B).rail(21, 0, 30, E).build()
    );

    list = filter.filter(list);

    // Then: expect correct alerts to be added
    Itinerary first = list.get(0);
    assertEquals(1, first.getLegs().get(0).getTransitAlerts().size());
    assertEquals(ID, first.getLegs().get(0).getTransitAlerts().iterator().next().getId());
    assertEquals(0, list.get(1).getLegs().get(0).getTransitAlerts().size());
  }
}
