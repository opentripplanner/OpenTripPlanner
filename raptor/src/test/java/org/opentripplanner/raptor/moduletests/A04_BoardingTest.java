package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.multiCriteria;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.standard;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorTestFactory;
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
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = data.requestBuilder();
  private final RaptorService<TestTripSchedule> raptorService = RaptorTestFactory.raptorService();

  @BeforeEach
  void setup() {
    data
      .access("Walk 1m ~ A")
      .withTimetables(
        """
        -- R1
        A     B
        0:10  0:18
        -- R2
        A           C
        0:14        0:18
        -- R3
        A                 D
        0:12              0:18

        -- R4
              B     C     D     E     F     G
              0:20  0:21  0:22  0:30  0:31  0:32
        -- R5
                                E                 H
                                0:35              0:42
        -- R6
                                      F           H
                                      0:35        0:40
        -- R7
                                            G     H
                                            0:35  0:44
        """
      )
      .egress("H ~ Walk 1m");

    requestBuilder
      .searchParams()
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T01_00)
      .searchOneIterationOnly();
    ModuleTestDebugLogging.setupDebugLogging(data);
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
        "~ BUS R1 0:10 0:18 ~ B " +
        "~ BUS R4 0:20 0:31 ~ F " +
        "~ BUS R6 0:35 0:40 ~ H " +
        "~ Walk 1m [0:09 0:41 32m Tₙ2]"
      )
      // A reverse test on the standard profile is included to demonstrate
      // that the min-travel-duration and the standard give different results
      // when searching in reverse. The L2 alight stop is different.
      .add(
        standard().reverseOnly(),
        // Searching in REVERSE we will "board" L2 at the first possible stop G and "alight" at the
        // optimal stop C (the best "arrival-time").
        "Walk 1m ~ A " +
        "~ BUS R2 0:14 0:18 ~ C " +
        "~ BUS R4 0:21 0:32 ~ G " +
        "~ BUS R7 0:35 0:44 ~ H " +
        "~ Walk 1m [0:13 0:45 32m Tₙ2]"
      )
      .add(
        multiCriteria(),
        // Board L2 at stop C and alight at stop F
        "Walk 1m ~ A " +
        "~ BUS R2 0:14 0:18 ~ C " +
        "~ BUS R4 0:21 0:31 ~ F " +
        "~ BUS R6 0:35 0:40 ~ H " +
        "~ Walk 1m [0:13 0:41 28m Tₙ2 C₁3_600]"
      )
      .build();
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void testRaptor(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }
}
