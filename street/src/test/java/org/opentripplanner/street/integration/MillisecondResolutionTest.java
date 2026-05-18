package org.opentripplanner.street.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.path.StreetPath;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.request.StreetSearchRequest;

class MillisecondResolutionTest extends GraphRoutingTest {

  private StreetVertex A;
  private StreetVertex B;

  @BeforeEach
  void setUp() {
    modelOf(
      new Builder() {
        @Override
        public void build() {
          A = intersection("A", 59.94646, 10.77511);
          B = intersection("B", 59.94641, 10.77522);
          street(A, B, 15, StreetTraversalPermission.ALL, StreetTraversalPermission.ALL);
        }
      }
    );
  }

  /**
   * Verify that both depart-at and arrive-by routing produce identical millisecond-accurate trip
   * durations regardless of the sub-second component of the request time.
   */
  @ParameterizedTest
  @ValueSource(ints = { 0, 200, 400, 499, 500, 501, 600, 700, 800, 900, 999 })
  void pathReversalWorks(int offset) {
    var base = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    var time = base.plusMillis(offset);

    var forwardPaths = route(A, B, time, false);
    assertEquals(1, forwardPaths.size());
    var forwardStates = forwardPaths.getFirst().states();
    var forwardDiff = ChronoUnit.MILLIS.between(
      forwardStates.getFirst().getTimeAccurate(),
      forwardStates.getLast().getTimeAccurate()
    );

    var backwardPaths = route(A, B, time, true);
    assertEquals(1, backwardPaths.size());
    var backwardStates = backwardPaths.getFirst().states();
    var backwardDiff = ChronoUnit.MILLIS.between(
      backwardStates.getFirst().getTimeAccurate(),
      backwardStates.getLast().getTimeAccurate()
    );

    // Math.ceil(1000.0 * 15m / 1.33 m/s), same for every parametrized offset
    int expected = 11279;
    assertEquals(expected, forwardDiff);
    assertEquals(expected, backwardDiff);
  }

  private List<StreetPath> route(
    StreetVertex from,
    StreetVertex to,
    Instant time,
    boolean arriveBy
  ) {
    var request = StreetSearchRequest.of()
      .withStartTime(time)
      .withMode(StreetMode.WALK)
      .withArriveBy(arriveBy)
      .build();

    return StreetSearchBuilder.of()
      .withHeuristic(new EuclideanRemainingWeightHeuristic())
      .withRequest(request)
      .withFrom(from)
      .withTo(to)
      .getPathsToTarget();
  }
}
