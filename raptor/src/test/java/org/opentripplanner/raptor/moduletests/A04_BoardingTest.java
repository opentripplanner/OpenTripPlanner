package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.multiCriteria;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.standard;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.moduletests.support.ModuleTestDebugLogging;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCase;

/**
 * FEATURE UNDER TEST
 * <p>
 * This test focus on boarding a trip after a transfer at different pattern stop positions. The
 * routing strategies do not board at the same stop, they should board at the optimal stop for the
 * criterion which they optimize on.
 * <ul>
 *    <li>The `MinTravelDurationRoutingStrategy` should board at the stop which give the shortest
 *    travel duration. Hence; picking the path with the shortest travel duration at the point of
 *    boarding.
 *    <li>The `ArrivalTimeRoutingStrategy` should board at the first possible stop and drop all
 *    other paths boarding the same trip. We only care about the arrival-time with this strategy so
 *    any boarding of the same trip will lead to the same arrival-time at the destination. Picking
 *    the first possible path gives us deterministic behavior and the best performance.
 *    <li>The `McTransitWorker` should board at the all stops which give us pareto optimal results.
 *    The test is set up, so that the cost and time is aligned. We expect the same result as with
 *    the `MinTravelDurationRoutingStrategy`. There are other tests that focus on testing the
 *    multi-criteria routing strategy.
 * </ul>
 * Note! This test does only one iteration, it does not run RangeRaptor over a time window. This
 * would give us the best/latest departure time for the `ArrivalTimeRoutingStrategy` as well, but
 * that is not in scope for this test.
 */
@SuppressWarnings("FieldCanBeLocal")
public class A04_BoardingTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder =
    new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  void setup() {
    // There is three possible paths before boarding trip L2
    data.withRoute(route("L1_1", STOP_A, STOP_B).withTimetable(schedule("0:10 0:18")));
    data.withRoute(route("L1_2", STOP_A, STOP_C).withTimetable(schedule("0:14 0:18")));
    data.withRoute(route("L1_3", STOP_A, STOP_D).withTimetable(schedule("0:12 0:18")));

    data.withRoute(
      route("L2", STOP_B, STOP_C, STOP_D, STOP_E, STOP_F, STOP_G).withTimetable(
        schedule("0:20 0:21 0:22 0:30 0:31 0:32")
      )
    );

    // There is three possible paths from trip L2 to the destination. These paths are used to test
    // the reverse search.
    data.withRoute(route("L3_1", STOP_E, STOP_H).withTimetable(schedule("0:35 0:42")));
    data.withRoute(route("L3_2", STOP_F, STOP_H).withTimetable(schedule("0:35 0:40")));
    data.withRoute(route("L3_3", STOP_G, STOP_H).withTimetable(schedule("0:35 0:44")));

    requestBuilder
      .searchParams()
      .addAccessPaths(TestAccessEgress.walk(STOP_A, D1m))
      .addEgressPaths(TestAccessEgress.walk(STOP_H, D1m))
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T01_00)
      .searchOneIterationOnly();
    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  static List<RaptorModuleTestCase> testCases() {
    return RaptorModuleTestCase.of()
      .addMinDuration("23m", TX_2, T00_00, T01_00)
      // A test on the standard profile is included to demonstrate that the
      // min-travel-duration and the standard give different results. The L2
      // boarding stop is different.
      .add(
        standard().forwardOnly(),
        // Board L2 at first possible stop B (not C) and arrive at F (the earliest arrival time)
        "Walk 1m ~ A " +
        "~ BUS L1_1 0:10 0:18 ~ B " +
        "~ BUS L2 0:20 0:31 ~ F " +
        "~ BUS L3_2 0:35 0:40 ~ H " +
        "~ Walk 1m [0:09 0:41 32m Tₓ2]"
      )
      // A reverse test on the standard profile is included to demonstrate
      // that the min-travel-duration and the standard give different results
      // when searching in reverse. The L2 alight stop is different.
      .add(
        standard().reverseOnly(),
        // Searching in REVERSE we will "board" L2 at the first possible stop G and "alight" at the
        // optimal stop C (the best "arrival-time").
        "Walk 1m ~ A " +
        "~ BUS L1_2 0:14 0:18 ~ C " +
        "~ BUS L2 0:21 0:32 ~ G " +
        "~ BUS L3_3 0:35 0:44 ~ H " +
        "~ Walk 1m [0:13 0:45 32m Tₓ2]"
      )
      .add(
        multiCriteria(),
        // Board L2 at stop C and alight at stop F
        "Walk 1m ~ A " +
        "~ BUS L1_2 0:14 0:18 ~ C " +
        "~ BUS L2 0:21 0:31 ~ F " +
        "~ BUS L3_2 0:35 0:40 ~ H " +
        "~ Walk 1m [0:13 0:41 28m Tₓ2 C₁3_600]"
      )
      .build();
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void testRaptor(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }
}
