package org.opentripplanner.routing.algorithm.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.TripPlan;
import org.opentripplanner.routing.api.request.RouteRequest;

class TripPlanMapperTest {

  private static final Instant DATETIME = Instant.EPOCH;
  private static final GenericLocation FROM = GenericLocation.fromCoordinate(0, 0);
  private static final GenericLocation TO = GenericLocation.fromCoordinate(1, 1);

  @Test
  void mapRequestWithNoItineraries() {
    RouteRequest request = RouteRequest.of()
      .withFrom(FROM)
      .withTo(TO)
      .withDateTime(DATETIME)
      .buildRequest();

    TripPlan tripPlan = TripPlanMapper.mapTripPlan(request, List.of());

    assertNotNull(tripPlan);
    assertEquals(DATETIME, tripPlan.date);
    assertEquals(new WgsCoordinate(FROM.getCoordinate()), (tripPlan.from.coordinate));
    assertEquals(new WgsCoordinate(TO.getCoordinate()), (tripPlan.to.coordinate));
    assertTrue(tripPlan.itineraries.isEmpty());
  }
}
