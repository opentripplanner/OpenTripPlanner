package org.opentripplanner.routing.graphfinder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.search.state.TestStateBuilder;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.site.RegularStop;

class StopFinderTraverseVisitorTest {

  static final RegularStop STOP = TransitModelForTest.of().stop("a-stop", 1, 1).build();

  @Test
  void deduplicateStops() {
    var visitor = new StopFinderTraverseVisitor(1000);

    assertEquals(List.of(), visitor.stopsFound());
    var state1 = TestStateBuilder.ofWalking().streetEdge().stop(STOP).build();

    visitor.visitVertex(state1);

    var transitStopVertex = (TransitStopVertex) state1.getVertex();
    final NearbyStop nearbyStop = NearbyStop.nearbyStopForState(
      state1,
      transitStopVertex.getStop()
    );

    assertEquals(List.of(nearbyStop), visitor.stopsFound());

    visitor.visitVertex(state1);

    assertEquals(List.of(nearbyStop), visitor.stopsFound());

    // taking a different path to the same stop
    var state2 = TestStateBuilder.ofWalking().streetEdge().streetEdge().stop(STOP).build();

    visitor.visitVertex(state2);

    assertEquals(List.of(nearbyStop), visitor.stopsFound());
  }
}
