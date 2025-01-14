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
import org.opentripplanner.routing.alertpatch.TransitAlertBuilder;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.TimetableRepository;

class DecorateTransitAlertTest implements PlanTestConstants {

  private static final FeedScopedId ID = new FeedScopedId("FEED", "ALERT");

  @Test
  void testRoute() {
    final var transitAlertService = buildService(
      TransitAlert.of(ID).addEntity(new EntitySelector.Route(BUS_ROUTE.getId()))
    );

    var decorator = new DecorateTransitAlert(transitAlertService, ignore -> null);

    // Given a list with one itinerary
    var i1 = newItinerary(A).bus(31, 0, 30, E).build();
    decorator.decorate(i1);

    // Then: expect correct alerts to be added
    assertEquals(1, i1.getLegs().getFirst().getTransitAlerts().size());
    assertEquals(ID, i1.getLegs().getFirst().getTransitAlerts().iterator().next().getId());

    var i2 = newItinerary(B).rail(21, 0, 30, E).build();
    decorator.decorate(i2);

    assertEquals(0, i2.getLegs().getFirst().getTransitAlerts().size());
  }

  @Test
  void testStop() {
    final var transitAlertService = buildService(
      TransitAlert.of(ID).addEntity(new EntitySelector.Stop(E.stop.getId()))
    );

    var decorator = new DecorateTransitAlert(transitAlertService, ignore -> null);

    // Given a list with one itinerary
    var i1 = newItinerary(A).bus(31, 0, 30, E).build();
    decorator.decorate(i1);

    // Then: expect correct alerts to be added
    assertEquals(1, i1.getLegs().getFirst().getTransitAlerts().size());
    assertEquals(ID, i1.getLegs().getFirst().getTransitAlerts().iterator().next().getId());
  }

  private static TransitAlertServiceImpl buildService(TransitAlertBuilder builder) {
    var transitAlertService = new TransitAlertServiceImpl(new TimetableRepository());
    transitAlertService.setAlerts(
      List.of(builder.addTimePeriod(new TimePeriod(0, TimePeriod.OPEN_ENDED)).build())
    );
    return transitAlertService;
  }
}
