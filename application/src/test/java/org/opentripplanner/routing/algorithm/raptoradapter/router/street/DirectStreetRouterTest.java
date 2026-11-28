package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.street.model.StreetMode;

class DirectStreetRouterTest {

  private static final Duration MAX_DURATION = Duration.ofMinutes(10);
  private static final double WALK_SPEED = 1.5;
  private static final double WHEELCHAIR_SPEED = 0.5;

  @Test
  void walkDistanceLimitUsesTheWalkSpeed() {
    var request = request(false);
    assertEquals(600 * WALK_SPEED, DirectStreetRouter.calculateDistanceMaxLimit(request, 0f));
  }

  @Test
  void wheelchairDistanceLimitUsesTheWheelchairSpeed() {
    var request = request(true);
    assertEquals(600 * WHEELCHAIR_SPEED, DirectStreetRouter.calculateDistanceMaxLimit(request, 0f));
  }

  private static RouteRequest request(boolean wheelchair) {
    return RouteRequest.of()
      .withJourney(j -> j.withDirect(new StreetRequest(StreetMode.WALK)).withWheelchair(wheelchair))
      .withPreferences(p ->
        p
          .withStreet(s -> s.withMaxDirectDuration(d -> d.withDefault(MAX_DURATION)))
          .withWalk(w -> w.withSpeed(WALK_SPEED))
          .withWheelchair(w -> w.withSpeed(WHEELCHAIR_SPEED))
      )
      .buildDefault();
  }
}
