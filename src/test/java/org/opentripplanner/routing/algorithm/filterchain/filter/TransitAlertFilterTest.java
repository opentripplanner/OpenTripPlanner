package org.opentripplanner.routing.algorithm.filterchain.filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.opentripplanner.model.plan.TestItineraryBuilder.BUS_ROUTE;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.alertpatch.TimePeriod;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class TransitAlertFilterTest implements PlanTestConstants {

  @Test
  void testFilter() {
    TransitAlertFilter filter = new TransitAlertFilter(
      Mockito.spy(TestTransitAlertService.class),
      ignore -> null
    );

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
    assertEquals("BUS", first.getLegs().get(0).getTransitAlerts().iterator().next().getId());
    assertEquals(0, list.get(1).getLegs().get(0).getTransitAlerts().size());
  }

  abstract static class TestTransitAlertService implements TransitAlertService {

    public static final TransitAlert BUS_ALERT = new TransitAlert();

    static {
      BUS_ALERT.setId("BUS");
      BUS_ALERT.setTimePeriods(List.of(new TimePeriod(0, TimePeriod.OPEN_ENDED)));
    }

    @Override
    public Collection<TransitAlert> getRouteAlerts(FeedScopedId route) {
      if (route.equals(BUS_ROUTE.getId())) {
        return List.of(BUS_ALERT);
      }

      return List.of();
    }
  }
}
