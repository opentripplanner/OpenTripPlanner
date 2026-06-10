package org.opentripplanner.routing.algorithm.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.service.streetdetails.internal.DefaultStreetDetailsRepository;
import org.opentripplanner.service.streetdetails.internal.DefaultStreetDetailsService;
import org.opentripplanner.street.internal.notes.StreetNotesService;
import org.opentripplanner.street.model.path.StreetPath;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.TestStateBuilder;
import org.opentripplanner.transit.service.NoopSiteResolver;

class StreetPathToLegsMapperTest {

  @Test
  void testCarRentalPickUp() {
    var mapper = mapper();

    var state = TestStateBuilder.ofCarRental().streetEdge().pickUpCarFromStation().build();

    var legs = mapper.map(new StreetPath(state), RouteRequest.defaultValue());

    assertEquals(2, legs.size());
    assertEquals(TraverseMode.WALK, traverseMode(legs.get(0)));
    assertEquals(TraverseMode.CAR, traverseMode(legs.get(1)));
  }

  private TraverseMode traverseMode(Leg leg) {
    return ((StreetLeg) leg).getMode();
  }

  private StreetPathToLegsMapper mapper() {
    return new StreetPathToLegsMapper(
      new NoopSiteResolver(),
      ZoneIds.UTC,
      new StreetNotesService(),
      new DefaultStreetDetailsService(new DefaultStreetDetailsRepository()),
      1
    );
  }
}
