package org.opentripplanner.routing.algorithm.filterchain.filters.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.BUS_ROUTE;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.List;
import org.junit.jupiter.api.Test;
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

    var decorator = new DecorateTransitAlert(transitAlertService, ignore -> null);

    // Given a list with one itinerary
    var i1 = newItinerary(A).bus(31, 0, 30, E).build();
    decorator.decorate(i1);

    // Then: expect correct alerts to be added
    assertEquals(1, i1.getLegs().get(0).getTransitAlerts().size());
    assertEquals(ID, i1.getLegs().get(0).getTransitAlerts().iterator().next().getId());

    var i2 = newItinerary(B).rail(21, 0, 30, E).build();
    decorator.decorate(i2);

    assertEquals(0, i2.getLegs().get(0).getTransitAlerts().size());
  }
}
