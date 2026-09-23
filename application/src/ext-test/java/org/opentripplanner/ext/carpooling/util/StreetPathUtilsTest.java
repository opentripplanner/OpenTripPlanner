package org.opentripplanner.ext.carpooling.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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

  private static final Duration TEN_MINUTES = Duration.ofMinutes(10);
  private static final Duration ONE_MINUTE = Duration.ofMinutes(1);

  @Test
  void calculateCumulativeDurations_sixSegmentsWithStopDelay() {
    Duration[] segments = {
      TEN_MINUTES,
      TEN_MINUTES,
      TEN_MINUTES,
      TEN_MINUTES,
      TEN_MINUTES,
      TEN_MINUTES,
    };

    Duration[] result = StreetPathUtils.calculateCumulativeDurations(segments, ONE_MINUTE);

    assertArrayEquals(
      new Duration[] {
        Duration.ofMinutes(0),
        Duration.ofMinutes(10),
        Duration.ofMinutes(21),
        Duration.ofMinutes(32),
        Duration.ofMinutes(43),
        Duration.ofMinutes(54),
        Duration.ofMinutes(65),
      },
      result
    );
  }

  @Test
  void calculateCumulativeDurations_noStopDelay() {
    Duration[] segments = { TEN_MINUTES, TEN_MINUTES, TEN_MINUTES };

    Duration[] result = StreetPathUtils.calculateCumulativeDurations(segments, Duration.ZERO);

    assertArrayEquals(
      new Duration[] {
        Duration.ofMinutes(0),
        Duration.ofMinutes(10),
        Duration.ofMinutes(20),
        Duration.ofMinutes(30),
      },
      result
    );
  }

  @Test
  void calculateCumulativeDurations_singleSegment_noStopDelayApplied() {
    Duration[] segments = { TEN_MINUTES };

    Duration[] result = StreetPathUtils.calculateCumulativeDurations(segments, ONE_MINUTE);

    assertArrayEquals(new Duration[] { Duration.ZERO, TEN_MINUTES }, result);
  }

  @Test
  void calculateCumulativeDurations_twoSegments_stopDelayOnlyAtSecondPoint() {
    Duration[] segments = { TEN_MINUTES, TEN_MINUTES };

    Duration[] result = StreetPathUtils.calculateCumulativeDurations(segments, ONE_MINUTE);

    assertArrayEquals(
      new Duration[] { Duration.ofMinutes(0), Duration.ofMinutes(10), Duration.ofMinutes(21) },
      result
    );
  }

  @Test
  void calculateCumulativeDurations_noSegments() {
    Duration[] segments = {};

    Duration[] result = StreetPathUtils.calculateCumulativeDurations(segments, ONE_MINUTE);

    assertArrayEquals(new Duration[] { Duration.ZERO }, result);
  }

  @Test
  void calculateCumulativeDurations_varyingSegmentDurations() {
    Duration[] segments = { Duration.ofMinutes(5), Duration.ofMinutes(15), Duration.ofMinutes(10) };

    Duration[] result = StreetPathUtils.calculateCumulativeDurations(
      segments,
      Duration.ofMinutes(2)
    );

    assertEquals(Duration.ofMinutes(0), result[0]);
    assertEquals(Duration.ofMinutes(5), result[1]);
    assertEquals(Duration.ofMinutes(22), result[2]);
    assertEquals(Duration.ofMinutes(34), result[3]);
  }

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
