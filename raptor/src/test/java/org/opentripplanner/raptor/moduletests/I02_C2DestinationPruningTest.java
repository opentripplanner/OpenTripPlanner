package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.free;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.api.request.TestGroupPriorityCalculator.GROUP_A;
import static org.opentripplanner.raptor.api.request.TestGroupPriorityCalculator.GROUP_B;
import static org.opentripplanner.raptor.api.request.TestGroupPriorityCalculator.GROUP_C;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestRoute;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripPattern;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.api.request.TestGroupPriorityCalculator;
import org.opentripplanner.raptor.configure.RaptorTestFactory;

/**
 * FEATURE UNDER TEST
 * <p>
 * Test that the c2 cost criterion is correctly used in destination pruning with heuristics.
 * The HeuristicsProvider should pass c2 values through to the DestinationArrivalPaths.qualify()
 * method, which uses c2 in pareto comparison to determine if a path is dominated.
 * <p>
 * This test verifies that paths are correctly qualified based on c2 values when using
 * multi-criteria search with destination pruning optimization (PARETO_CHECK_AGAINST_DESTINATION).
 */
public class I02_C2DestinationPruningTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder =
    new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = RaptorTestFactory.raptorService();

  /// Test scenario with four routes that create paths with different c2 values.
  /// The `relaxC1` is set to 10% with TransitPriorityCalculator enabled.
  ///
  /// - R1 is the best option based on time and c1.
  /// - R1 & R2 both have the same priorityGroup; Hence, R1 dominates R2 since it is 1s faster.
  /// - R1 & R3 have different priorityGroups and c2 for R3 is 9.9% worse => R1 does not dominate R3.
  /// - R1 & R4 have different priorityGroups and c2 for R4 is 10.2% worse => R1 does dominate R4.
  ///
  /// With destination pruning enabled, the heuristics should correctly pass c2 values
  /// to qualify paths, ensuring that paths are not incorrectly pruned based on incomplete
  /// cost information.
  @Test
  @DisplayName("C2 destination pruning with transit priority groups")
  void c2DestinationPruningWithTransitPriority() {
    var r1 = routeA2B("R1", GROUP_B, "00:05 00:10:01");
    var r2 = routeA2B("R2", GROUP_B, "00:05 00:10:02");
    var r3 = routeA2B("R3", GROUP_C, "00:05 00:10:31");
    var r4 = routeA2B("R4", GROUP_A, "00:05 00:10:32");

    data.withRoutes(r1, r2, r3, r4).withBoardCost(0);

    requestBuilder
      .searchParams()
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T01_00)
      .addAccessPaths(free(STOP_A))
      .addEgressPaths(free(STOP_B));

    requestBuilder.withMultiCriteria(mc ->
      mc
        .withRelaxC1(value -> ((value * 110) / 100))
        .withTransitPriorityCalculator(new TestGroupPriorityCalculator())
    );

    // Verify that R1 and R3 are included (R2 is dominated by R1, R4 is dominated by R1)
    assertEquals(
      """
      A ~ BUS R1 0:05 0:10:01 ~ B [0:05 0:10:01 5m1s Tₙ0 C₁301 C₂2]
      A ~ BUS R3 0:05 0:10:31 ~ B [0:05 0:10:31 5m31s Tₙ0 C₁331 C₂4]""",
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }

  private static TestRoute routeA2B(String name, int priorityGroup, String timetable) {
    return route(
      TestTripPattern.of(name, STOP_A, STOP_B).priorityGroup(priorityGroup).build()
    ).withTimetable(schedule(timetable));
  }
}
