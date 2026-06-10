package org.opentripplanner.ext.carpooling.constraints;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.util.GraphPathUtils.calculateCumulativeDurations;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.CarpoolGraphPathBuilder;
import org.opentripplanner.ext.carpooling.model.CarpoolStop;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

class PassengerDelayConstraintsTest {

  private static final AtomicInteger STOP_COUNTER = new AtomicInteger(0);
  private static final Duration FIVE_MINUTES = Duration.ofMinutes(5);

  private static CarpoolStop stopWithBudget(Duration budget) {
    return CarpoolStop.of(FeedScopedId.ofNullable("TEST", "stop-" + STOP_COUNTER.incrementAndGet()))
      .withCoordinate(new org.opentripplanner.street.geometry.WgsCoordinate(59.9, 10.7))
      .withDeviationBudget(budget)
      .build();
  }

  @Test
  void satisfiesConstraints_delayWellUnderBudget_accepts() {
    Duration[] originalTimes = { Duration.ZERO, Duration.ofMinutes(5), Duration.ofMinutes(15) };
    var stops = List.of(
      stopWithBudget(FIVE_MINUTES),
      stopWithBudget(FIVE_MINUTES),
      stopWithBudget(FIVE_MINUTES)
    );

    // Stop1 delay: 7min - 5min = 2min (within 5min budget)
    // Destination delay: 17min - 15min = 2min (within 5min budget)
    GraphPath<State, Edge, Vertex>[] modifiedSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(3)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(4)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
    };

    assertTrue(
      PassengerDelayConstraints.satisfiesConstraints(
        originalTimes,
        calculateCumulativeDurations(modifiedSegments, Duration.ZERO),
        1,
        3,
        stops
      )
    );
  }

  @Test
  void satisfiesConstraints_delayExactlyAtBudget_accepts() {
    Duration[] originalTimes = { Duration.ZERO, Duration.ofMinutes(10), Duration.ofMinutes(20) };
    var stops = List.of(
      stopWithBudget(FIVE_MINUTES),
      stopWithBudget(FIVE_MINUTES),
      stopWithBudget(FIVE_MINUTES)
    );

    // Stop1 delay: 15min - 10min = 5min (exactly at 5min budget)
    GraphPath<State, Edge, Vertex>[] modifiedSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(10)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
    };

    assertTrue(
      PassengerDelayConstraints.satisfiesConstraints(
        originalTimes,
        calculateCumulativeDurations(modifiedSegments, Duration.ZERO),
        1,
        3,
        stops
      )
    );
  }

  @Test
  void satisfiesConstraints_delayOverBudget_rejects() {
    Duration[] originalTimes = { Duration.ZERO, Duration.ofMinutes(10), Duration.ofMinutes(20) };
    var stops = List.of(
      stopWithBudget(FIVE_MINUTES),
      stopWithBudget(FIVE_MINUTES),
      stopWithBudget(FIVE_MINUTES)
    );

    // Stop1 delay: 16min - 10min = 6min (exceeds 5min budget)
    GraphPath<State, Edge, Vertex>[] modifiedSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(11)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
    };

    assertFalse(
      PassengerDelayConstraints.satisfiesConstraints(
        originalTimes,
        calculateCumulativeDurations(modifiedSegments, Duration.ZERO),
        1,
        3,
        stops
      )
    );
  }

  @Test
  void satisfiesConstraints_destinationOverBudget_rejects() {
    Duration[] originalTimes = { Duration.ZERO, Duration.ofMinutes(10), Duration.ofMinutes(20) };
    var stops = List.of(
      stopWithBudget(FIVE_MINUTES),
      stopWithBudget(Duration.ofMinutes(20)),
      stopWithBudget(FIVE_MINUTES)
    );

    // Stop1 delay: 12min - 10min = 2min (within 20min budget)
    // Destination delay: 27min - 20min = 7min (exceeds 5min budget)
    GraphPath<State, Edge, Vertex>[] modifiedSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(7)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(10)),
    };

    assertFalse(
      PassengerDelayConstraints.satisfiesConstraints(
        originalTimes,
        calculateCumulativeDurations(modifiedSegments, Duration.ZERO),
        1,
        3,
        stops
      )
    );
  }

  @Test
  void satisfiesConstraints_multipleStops_oneOverBudget_rejects() {
    Duration[] originalTimes = {
      Duration.ZERO,
      Duration.ofMinutes(10),
      Duration.ofMinutes(20),
      Duration.ofMinutes(30),
    };
    var stops = List.of(
      stopWithBudget(FIVE_MINUTES),
      stopWithBudget(FIVE_MINUTES),
      stopWithBudget(FIVE_MINUTES),
      stopWithBudget(FIVE_MINUTES)
    );

    // Stop1 delay: 13min - 10min = 3min ok
    // Stop2 delay: 27min - 20min = 7min exceeds
    GraphPath<State, Edge, Vertex>[] modifiedSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(8)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(9)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
    };

    assertFalse(
      PassengerDelayConstraints.satisfiesConstraints(
        originalTimes,
        calculateCumulativeDurations(modifiedSegments, Duration.ZERO),
        1,
        3,
        stops
      )
    );
  }

  @Test
  void satisfiesConstraints_multipleStops_allUnderBudget_accepts() {
    Duration[] originalTimes = {
      Duration.ZERO,
      Duration.ofMinutes(10),
      Duration.ofMinutes(20),
      Duration.ofMinutes(30),
    };
    var stops = List.of(
      stopWithBudget(FIVE_MINUTES),
      stopWithBudget(FIVE_MINUTES),
      stopWithBudget(FIVE_MINUTES),
      stopWithBudget(FIVE_MINUTES)
    );

    GraphPath<State, Edge, Vertex>[] modifiedSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(7)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(7)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(10)),
    };

    assertTrue(
      PassengerDelayConstraints.satisfiesConstraints(
        originalTimes,
        calculateCumulativeDurations(modifiedSegments, Duration.ZERO),
        1,
        3,
        stops
      )
    );
  }

  @Test
  void satisfiesConstraints_differentBudgetsPerStop() {
    Duration[] originalTimes = { Duration.ZERO, Duration.ofMinutes(10), Duration.ofMinutes(20) };
    // Stop 1 has 2min budget, destination has 10min budget
    var stops = List.of(
      stopWithBudget(FIVE_MINUTES),
      stopWithBudget(Duration.ofMinutes(2)),
      stopWithBudget(Duration.ofMinutes(10))
    );

    // Stop1 delay: 13min - 10min = 3min (exceeds 2min budget)
    GraphPath<State, Edge, Vertex>[] modifiedSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(8)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
    };

    assertFalse(
      PassengerDelayConstraints.satisfiesConstraints(
        originalTimes,
        calculateCumulativeDurations(modifiedSegments, Duration.ZERO),
        1,
        3,
        stops
      )
    );
  }

  @Test
  void satisfiesConstraints_noDelay_accepts() {
    Duration[] originalTimes = { Duration.ZERO, Duration.ofMinutes(10), Duration.ofMinutes(20) };
    var stops = List.of(
      stopWithBudget(FIVE_MINUTES),
      stopWithBudget(FIVE_MINUTES),
      stopWithBudget(FIVE_MINUTES)
    );

    GraphPath<State, Edge, Vertex>[] modifiedSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(4)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(6)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
    };

    assertTrue(
      PassengerDelayConstraints.satisfiesConstraints(
        originalTimes,
        calculateCumulativeDurations(modifiedSegments, Duration.ZERO),
        1,
        3,
        stops
      )
    );
  }

  @Test
  void satisfiesConstraints_zeroBudget_rejectsAnyDelay() {
    Duration[] originalTimes = { Duration.ZERO, Duration.ofMinutes(10), Duration.ofMinutes(20) };
    var stops = List.of(
      stopWithBudget(Duration.ZERO),
      stopWithBudget(Duration.ZERO),
      stopWithBudget(Duration.ZERO)
    );

    // Stop1 delay: 10min + 1s - 10min = 1s (exceeds zero budget)
    GraphPath<State, Edge, Vertex>[] modifiedSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5).plusSeconds(1)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
    };

    assertFalse(
      PassengerDelayConstraints.satisfiesConstraints(
        originalTimes,
        calculateCumulativeDurations(modifiedSegments, Duration.ZERO),
        1,
        3,
        stops
      )
    );
  }

  @Test
  void satisfiesConstraints_zeroBudget_noDelay_accepts() {
    var stops = List.of(
      stopWithBudget(Duration.ZERO),
      stopWithBudget(Duration.ZERO),
      stopWithBudget(Duration.ZERO)
    );

    // Use the same GraphPaths to derive both original and modified times
    // so there is truly zero delay (avoids rounding from GraphPath construction)
    GraphPath<State, Edge, Vertex>[] modifiedSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(4)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(6)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
    };
    Duration[] cumulativeDurations = calculateCumulativeDurations(modifiedSegments, Duration.ZERO);

    // originalTimes = modified times at the original stop positions
    // With pickup=1, dropoff=3: original indices [0,1,2] map to modified [0,2,4]
    Duration[] originalTimes = {
      cumulativeDurations[0],
      cumulativeDurations[2],
      cumulativeDurations[4],
    };

    assertTrue(
      PassengerDelayConstraints.satisfiesConstraints(
        originalTimes,
        cumulativeDurations,
        1,
        3,
        stops
      )
    );
  }

  @Test
  void satisfiesConstraints_largeBudget_acceptsLargeDelay() {
    Duration[] originalTimes = { Duration.ZERO, Duration.ofMinutes(10), Duration.ofMinutes(20) };
    var stops = List.of(
      stopWithBudget(Duration.ZERO),
      stopWithBudget(Duration.ofHours(1)),
      stopWithBudget(Duration.ofHours(1))
    );

    // Stop1 delay: 40min - 10min = 30min (within 60min budget)
    GraphPath<State, Edge, Vertex>[] modifiedSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(35)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
    };

    assertTrue(
      PassengerDelayConstraints.satisfiesConstraints(
        originalTimes,
        calculateCumulativeDurations(modifiedSegments, Duration.ZERO),
        1,
        3,
        stops
      )
    );
  }

  @Test
  void satisfiesConstraints_tightAndPermissiveStops_respectsEachBudget() {
    Duration[] originalTimes = { Duration.ZERO, Duration.ofMinutes(10), Duration.ofMinutes(20) };
    // Stop 1 is strict (3min), destination is permissive (30min)
    var stops = List.of(
      stopWithBudget(Duration.ZERO),
      stopWithBudget(Duration.ofMinutes(3)),
      stopWithBudget(Duration.ofMinutes(30))
    );

    // Stop1 delay: 12min - 10min = 2min (within 3min budget, ok)
    // Destination delay: 47min - 20min = 27min (within 30min budget, ok)
    GraphPath<State, Edge, Vertex>[] modifiedSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(7)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(30)),
    };

    assertTrue(
      PassengerDelayConstraints.satisfiesConstraints(
        originalTimes,
        calculateCumulativeDurations(modifiedSegments, Duration.ZERO),
        1,
        3,
        stops
      )
    );
  }

  @Test
  void satisfiesConstraints_passengerBeforeAllStops_checksAllStops() {
    Duration[] originalTimes = {
      Duration.ZERO,
      Duration.ofMinutes(10),
      Duration.ofMinutes(20),
      Duration.ofMinutes(30),
    };
    var stops = List.of(
      stopWithBudget(FIVE_MINUTES),
      stopWithBudget(FIVE_MINUTES),
      stopWithBudget(FIVE_MINUTES),
      stopWithBudget(FIVE_MINUTES)
    );

    // Passenger inserted at very beginning (pickup at 1, dropoff at 2)
    // Stop1 delay: 13min - 10min = 3min ok
    // Stop2 delay: 24min - 20min = 4min ok
    // Destination delay: 36min - 30min = 6min exceeds 5min budget
    GraphPath<State, Edge, Vertex>[] modifiedSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(3)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(2)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(8)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(11)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(12)),
    };

    assertFalse(
      PassengerDelayConstraints.satisfiesConstraints(
        originalTimes,
        calculateCumulativeDurations(modifiedSegments, Duration.ZERO),
        1,
        2,
        stops
      )
    );
  }

  @Test
  void satisfiesConstraints_nonZeroStopDuration_countsDwellAtIntermediateStops() {
    // Uses a non-zero stopDuration to verify dwell at intermediate stops is included in the
    // budget check. The modified route has 2 extra dwells vs. the baseline (4 segments vs. 2),
    // which alone accounts for 2 of the 6-minute destination delay that pushes it over budget.
    Duration stopDuration = Duration.ofMinutes(1);
    var stops = List.of(
      stopWithBudget(FIVE_MINUTES),
      stopWithBudget(FIVE_MINUTES),
      stopWithBudget(FIVE_MINUTES)
    );

    // Baseline: 2 segments of 10min. With 1-min dwell: cumulative = [0, 10, 21]
    GraphPath<State, Edge, Vertex>[] baselineSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(10)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(10)),
    };
    Duration[] originalTimes = calculateCumulativeDurations(baselineSegments, stopDuration);

    // Modified (pickup=1, dropoff=3): 4 segments of 6min. With 1-min dwell: cumulative = [0, 6, 13, 20, 27]
    // Destination delay: 27 - 21 = 6min, exceeds 5min budget.
    GraphPath<State, Edge, Vertex>[] overBudgetSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(6)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(6)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(6)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(6)),
    };

    assertFalse(
      PassengerDelayConstraints.satisfiesConstraints(
        originalTimes,
        calculateCumulativeDurations(overBudgetSegments, stopDuration),
        1,
        3,
        stops
      )
    );

    // Shortening one segment to 5min: cumulative = [0, 6, 12, 19, 26]
    // Destination delay: 26 - 21 = 5min, exactly at budget → accepts.
    GraphPath<State, Edge, Vertex>[] atBudgetSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(6)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(5)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(6)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(6)),
    };

    assertTrue(
      PassengerDelayConstraints.satisfiesConstraints(
        originalTimes,
        calculateCumulativeDurations(atBudgetSegments, stopDuration),
        1,
        3,
        stops
      )
    );
  }

  @Test
  void satisfiesConstraints_passengerBetweenStops_checksAllStops() {
    Duration[] originalTimes = {
      Duration.ZERO,
      Duration.ofMinutes(10),
      Duration.ofMinutes(20),
      Duration.ofMinutes(30),
    };
    var stops = List.of(
      stopWithBudget(FIVE_MINUTES),
      stopWithBudget(FIVE_MINUTES),
      stopWithBudget(FIVE_MINUTES),
      stopWithBudget(FIVE_MINUTES)
    );

    // Passenger inserted between stops (pickup at 2, dropoff at 3)
    // Stop1 delay: 11min - 10min = 1min ok
    // Stop2 delay: 24min - 20min = 4min ok
    // Destination delay: 36min - 30min = 6min exceeds 5min budget
    GraphPath<State, Edge, Vertex>[] modifiedSegments = new GraphPath[] {
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(11)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(3)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(3)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(7)),
      CarpoolGraphPathBuilder.createGraphPath(Duration.ofMinutes(12)),
    };

    assertFalse(
      PassengerDelayConstraints.satisfiesConstraints(
        originalTimes,
        calculateCumulativeDurations(modifiedSegments, Duration.ZERO),
        2,
        3,
        stops
      )
    );
  }
}
