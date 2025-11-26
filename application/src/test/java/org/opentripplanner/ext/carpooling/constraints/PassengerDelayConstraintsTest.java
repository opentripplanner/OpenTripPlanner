package org.opentripplanner.ext.carpooling.constraints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.util.GraphPathUtils.calculateCumulativeDurations;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.ext.carpooling.CarpoolGraphPathBuilder;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

class PassengerDelayConstraintsTest {

  private PassengerDelayConstraints constraints;

  @BeforeEach
  void setup() {
    constraints = new PassengerDelayConstraints();
  }

  @Test
  void satisfiesConstraints_noExistingStops_alwaysAccepts() {
    Duration[] originalTimes = { Duration.ZERO, Duration.ofMinutes(10) };

    // Modified route with passenger inserted
    GraphPath<State, Edge, Vertex>[] modifiedSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(3)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(7)),
    };

    // Should accept - no existing passengers to protect
    assertTrue(
      constraints.satisfiesConstraints(
        originalTimes,
        calculateCumulativeDurations(modifiedSegments),
        1,
        2
      )
    );
  }

  @Test
  void satisfiesConstraints_delayWellUnderThreshold_accepts() {
    // Original timings: 0min -> 5min -> 15min
    Duration[] originalTimes = { Duration.ZERO, Duration.ofMinutes(5), Duration.ofMinutes(15) };

    // Modified route: boarding -> pickup -> stop1 -> dropoff -> alighting
    // Timings: 0min -> 3min -> 7min -> 12min -> 17min
    // Stop1 delay: 7min - 5min = 2min (well under 5min threshold)
    GraphPath<State, Edge, Vertex>[] modifiedSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(3)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(4)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
    };

    assertTrue(
      constraints.satisfiesConstraints(
        originalTimes,
        calculateCumulativeDurations(modifiedSegments),
        1,
        3
      )
    );
  }

  @Test
  void satisfiesConstraints_delayExactlyAtThreshold_accepts() {
    // Original route with one stop
    Duration[] originalTimes = { Duration.ZERO, Duration.ofMinutes(10), Duration.ofMinutes(20) };

    // Modified route where stop1 is delayed by exactly 5 minutes
    // Timings: 0min -> 5min -> 15min -> 20min -> 25min
    // Stop1 delay: 15min - 10min = 5min (exactly at threshold)
    GraphPath<State, Edge, Vertex>[] modifiedSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(10)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
    };

    assertTrue(
      constraints.satisfiesConstraints(
        originalTimes,
        calculateCumulativeDurations(modifiedSegments),
        1,
        3
      )
    );
  }

  @Test
  void satisfiesConstraints_delayOverThreshold_rejects() {
    // Original route with one stop
    Duration[] originalTimes = { Duration.ZERO, Duration.ofMinutes(10), Duration.ofMinutes(20) };

    // Modified route where stop1 is delayed by 6 minutes (over 5min threshold)
    // Timings: 0min -> 5min -> 16min -> 21min -> 26min
    // Stop1 delay: 16min - 10min = 6min (exceeds threshold)
    GraphPath<State, Edge, Vertex>[] modifiedSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(11)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
    };

    assertFalse(
      constraints.satisfiesConstraints(
        originalTimes,
        calculateCumulativeDurations(modifiedSegments),
        1,
        3
      )
    );
  }

  @Test
  void satisfiesConstraints_multipleStops_oneOverThreshold_rejects() {
    // Original route: boarding -> stop1 -> stop2 -> alighting
    Duration[] originalTimes = {
      Duration.ZERO,
      Duration.ofMinutes(10),
      Duration.ofMinutes(20),
      Duration.ofMinutes(30),
    };

    // Modified route where stop1 is ok (3min delay) but stop2 exceeds (7min delay)
    // Timings: 0min -> 5min -> 13min -> 18min -> 27min -> 32min
    // Stop1 delay: 13min - 10min = 3min ✓
    // Stop2 delay: 27min - 20min = 7min ✗
    GraphPath<State, Edge, Vertex>[] modifiedSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(8)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(9)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
    };

    assertFalse(
      constraints.satisfiesConstraints(
        originalTimes,
        calculateCumulativeDurations(modifiedSegments),
        1,
        3
      )
    );
  }

  @Test
  void satisfiesConstraints_multipleStops_allUnderThreshold_accepts() {
    // Original route: boarding -> stop1 -> stop2 -> alighting
    Duration[] originalTimes = {
      Duration.ZERO,
      Duration.ofMinutes(10),
      Duration.ofMinutes(20),
      Duration.ofMinutes(30),
    };

    // Modified route where both stops have acceptable delays
    // Timings: 0min -> 5min -> 12min -> 17min -> 24min -> 34min
    // Stop1 delay: 12min - 10min = 2min ✓
    // Stop2 delay: 24min - 20min = 4min ✓
    GraphPath<State, Edge, Vertex>[] modifiedSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(7)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(7)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(10)),
    };

    assertTrue(
      constraints.satisfiesConstraints(
        originalTimes,
        calculateCumulativeDurations(modifiedSegments),
        1,
        3
      )
    );
  }

  @Test
  void satisfiesConstraints_passengerBeforeAllStops_checksAllStops() {
    // Original route: boarding -> stop1 -> stop2 -> alighting
    Duration[] originalTimes = {
      Duration.ZERO,
      Duration.ofMinutes(10),
      Duration.ofMinutes(20),
      Duration.ofMinutes(30),
    };

    // Passenger inserted at very beginning (pickup at 1, dropoff at 2)
    // Modified: boarding -> pickup -> dropoff -> stop1 -> stop2 -> alighting
    // Mapping: stop1 (orig 1) -> mod 3, stop2 (orig 2) -> mod 4
    // Timings: 0min -> 3min -> 5min -> 13min -> 24min -> 34min
    // Stop1 delay: 13min - 10min = 3min ✓
    // Stop2 delay: 24min - 20min = 4min ✓
    GraphPath<State, Edge, Vertex>[] modifiedSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(3)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(2)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(8)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(11)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(10)),
    };

    assertTrue(
      constraints.satisfiesConstraints(
        originalTimes,
        calculateCumulativeDurations(modifiedSegments),
        1,
        2
      )
    );
  }

  @Test
  void satisfiesConstraints_passengerAfterAllStops_checksAllStops() {
    // Original route: boarding -> stop1 -> stop2 -> alighting
    Duration[] originalTimes = {
      Duration.ZERO,
      Duration.ofMinutes(10),
      Duration.ofMinutes(20),
      Duration.ofMinutes(30),
    };

    // Passenger inserted at very end (pickup at 3, dropoff at 4)
    // Modified: boarding -> stop1 -> stop2 -> pickup -> dropoff -> alighting
    // Mapping: stop1 (orig 1) -> mod 1, stop2 (orig 2) -> mod 2
    // Even though passenger comes after, routing to pickup might cause delays
    // Timings: 0min -> 11min -> 22min -> 27min -> 30min -> 40min
    // Stop1 delay: 11min - 10min = 1min ✓
    // Stop2 delay: 22min - 20min = 2min ✓
    GraphPath<State, Edge, Vertex>[] modifiedSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(11)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(11)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(3)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(10)),
    };

    assertTrue(
      constraints.satisfiesConstraints(
        originalTimes,
        calculateCumulativeDurations(modifiedSegments),
        3,
        4
      )
    );
  }

  @Test
  void satisfiesConstraints_passengerBetweenStops_checksAllStops() {
    // Original route: boarding -> stop1 -> stop2 -> alighting
    Duration[] originalTimes = {
      Duration.ZERO,
      Duration.ofMinutes(10),
      Duration.ofMinutes(20),
      Duration.ofMinutes(30),
    };

    // Passenger inserted between stops (pickup at 2, dropoff at 3)
    // Modified: boarding -> stop1 -> pickup -> dropoff -> stop2 -> alighting
    // Mapping: stop1 (orig 1) -> mod 1, stop2 (orig 2) -> mod 4
    // Timings: 0min -> 11min -> 14min -> 17min -> 24min -> 34min
    // Stop1 delay: 11min - 10min = 1min ✓
    // Stop2 delay: 24min - 20min = 4min ✓
    GraphPath<State, Edge, Vertex>[] modifiedSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(11)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(3)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(3)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(7)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(10)),
    };

    assertTrue(
      constraints.satisfiesConstraints(
        originalTimes,
        calculateCumulativeDurations(modifiedSegments),
        2,
        3
      )
    );
  }

  @Test
  void customMaxDelay_acceptsWithinCustomThreshold() {
    var customConstraints = new PassengerDelayConstraints(Duration.ofMinutes(10));

    Duration[] originalTimes = { Duration.ZERO, Duration.ofMinutes(10), Duration.ofMinutes(20) };

    // Stop1 delayed by 8 minutes (within 10min custom threshold)
    GraphPath<State, Edge, Vertex>[] modifiedSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(13)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
    };

    assertTrue(
      customConstraints.satisfiesConstraints(
        originalTimes,
        calculateCumulativeDurations(modifiedSegments),
        1,
        3
      )
    );
  }

  @Test
  void customMaxDelay_rejectsOverCustomThreshold() {
    var customConstraints = new PassengerDelayConstraints(Duration.ofMinutes(2));

    Duration[] originalTimes = { Duration.ZERO, Duration.ofMinutes(10), Duration.ofMinutes(20) };

    // Stop1 delayed by 3 minutes (over 2min custom threshold)
    GraphPath<State, Edge, Vertex>[] modifiedSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(8)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
    };

    assertFalse(
      customConstraints.satisfiesConstraints(
        originalTimes,
        calculateCumulativeDurations(modifiedSegments),
        1,
        3
      )
    );
  }

  @Test
  void customMaxDelay_zeroTolerance_rejectsAnyDelay() {
    var strictConstraints = new PassengerDelayConstraints(Duration.ZERO);

    Duration[] originalTimes = { Duration.ZERO, Duration.ofMinutes(10), Duration.ofMinutes(20) };

    // Stop1 delayed by even 1 second
    GraphPath<State, Edge, Vertex>[] modifiedSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5).plusSeconds(1)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
    };

    assertFalse(
      strictConstraints.satisfiesConstraints(
        originalTimes,
        calculateCumulativeDurations(modifiedSegments),
        1,
        3
      )
    );
  }

  @Test
  void customMaxDelay_veryPermissive_acceptsLargeDelays() {
    var permissiveConstraints = new PassengerDelayConstraints(Duration.ofHours(1));

    Duration[] originalTimes = { Duration.ZERO, Duration.ofMinutes(10), Duration.ofMinutes(20) };

    // Stop1 delayed by 30 minutes (well within 1 hour threshold)
    GraphPath<State, Edge, Vertex>[] modifiedSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(35)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
    };

    assertTrue(
      permissiveConstraints.satisfiesConstraints(
        originalTimes,
        calculateCumulativeDurations(modifiedSegments),
        1,
        3
      )
    );
  }

  @Test
  void getMaxDelay_returnsConfiguredValue() {
    assertEquals(Duration.ofMinutes(5), constraints.getMaxDelay());

    var customConstraints = new PassengerDelayConstraints(Duration.ofMinutes(10));
    assertEquals(Duration.ofMinutes(10), customConstraints.getMaxDelay());
  }

  @Test
  void defaultMaxDelay_isFiveMinutes() {
    assertEquals(Duration.ofMinutes(5), PassengerDelayConstraints.DEFAULT_MAX_DELAY);
  }

  @Test
  void constructor_negativeDelay_throwsException() {
    assertThrows(IllegalArgumentException.class, () ->
      new PassengerDelayConstraints(Duration.ofMinutes(-1))
    );
  }

  @Test
  void satisfiesConstraints_noDelay_accepts() {
    // Route where insertion doesn't add any delay
    Duration[] originalTimes = { Duration.ZERO, Duration.ofMinutes(10), Duration.ofMinutes(20) };

    // Modified route where stop1 arrives at exactly the same time
    // (perfect routing somehow)
    GraphPath<State, Edge, Vertex>[] modifiedSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(4)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(6)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
    };

    assertTrue(
      constraints.satisfiesConstraints(
        originalTimes,
        calculateCumulativeDurations(modifiedSegments),
        1,
        3
      )
    );
  }

  @Test
  void satisfiesConstraints_tripWithManyStops_checksAll() {
    // Original route with 5 stops
    Duration[] originalTimes = {
      Duration.ZERO,
      Duration.ofMinutes(10),
      Duration.ofMinutes(20),
      Duration.ofMinutes(30),
      Duration.ofMinutes(40),
      Duration.ofMinutes(50),
      Duration.ofMinutes(60),
    };

    // Insert passenger between stop2 and stop3 (positions 3, 4)
    // All stops should have delays <= 5 minutes
    // Modified indices: 0,1,2,pickup@3,dropoff@4,3,4,5,6
    // Note: With real State objects, durations will be slightly longer due to rounding
    // (typically 1-3 seconds per path). We use slightly shorter durations to ensure
    // the cumulative delays stay within the 5-minute threshold.
    GraphPath<State, Edge, Vertex>[] modifiedSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(10)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(10)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(3)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(2)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(8)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(10)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(10)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(10)),
    };

    assertTrue(
      constraints.satisfiesConstraints(
        originalTimes,
        calculateCumulativeDurations(modifiedSegments),
        3,
        4
      )
    );
  }
}
