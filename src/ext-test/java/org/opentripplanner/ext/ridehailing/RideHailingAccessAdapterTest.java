package org.opentripplanner.ext.ridehailing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.DefaultAccessEgress;
import org.opentripplanner.street.search.state.TestStateBuilder;

class RideHailingAccessAdapterTest {

  @Test
  void shiftTime() {
    var state = TestStateBuilder.ofWalking().streetEdge().streetEdge().streetEdge().build();
    assertNotNull(state);
    var access = new DefaultAccessEgress(0, state);
    var adapter = new RideHailingAccessAdapter(access, Duration.ofMinutes(10));
    var requestedStartTime = LocalTime.of(13, 0);
    var shiftedDeparture = LocalTime.ofSecondOfDay(
      adapter.earliestDepartureTime(requestedStartTime.toSecondOfDay())
    );
    assertEquals(LocalTime.of(13, 10), shiftedDeparture);

    var shiftedArrival = LocalTime.ofSecondOfDay(
      adapter.latestArrivalTime(requestedStartTime.plusMinutes(20).toSecondOfDay())
    );
    assertEquals(LocalTime.of(13, 30), shiftedArrival);
  }
}
