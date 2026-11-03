package org.opentripplanner.ext.carpooling.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.street.model.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.model.StreetModelForTest.streetEdge;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.path.StreetPath;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.EdgeTraverser;

class StreetPathUtilsTest {

  @Test
  void aReplayedWalkTakesThePassengersOwnSpeed() {
    var walk = walk(100);
    var request = StreetSearchRequest.of()
      .withWalk(w -> w.withSpeed(2.0))
      .build();
    assertEquals(Duration.ofSeconds(50), StreetPathUtils.replay(walk, request).duration());
  }

  /** A walk at the default speed over one edge of the given length. */
  private static StreetPath walk(double meters) {
    var edge = streetEdge(
      intersectionVertex("A", 59.0, 10.0),
      intersectionVertex("B", 59.001, 10.0),
      meters,
      StreetTraversalPermission.PEDESTRIAN
    );
    var request = StreetSearchRequest.of().withMode(StreetMode.WALK).build();
    return EdgeTraverser.createPath(List.of(edge), request).orElseThrow();
  }
}
