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
 * This test is similar to the {@link A04_BoardingTest}, except that this test focus on boarding
 * after access and alighting before egress, not after/before other transit legs. The setup is a bit
 * simpler since we only need one Route with three access and egress paths.
 * <p>
 * The routing strategies do not board at the same stop, they should board at the optimal stop for
 * the criterion which they optimize on. Read the doc for {@link A04_BoardingTest} which describe
 * the expected behavior with respect to boarding.
 * <p>
 * Note! This test run one iteration. It does not run RangeRaptor over a time window.
 */
@SuppressWarnings("FieldCanBeLocal")
public class B04_AccessEgressBoardingTest implements RaptorTestConstants {

  /** Board R1 at first possible stop A (not B) and arrive at stop E (the earliest arrival time) */
  private static final String EXP_PATH_BEST_ARRIVAL_TIME =
    "Walk 1s ~ A ~ BUS R1 0:10 0:34 ~ E ~ Walk 10s [0:09:59 0:34:10 24m11s Tₓ0]";

  /**
   * Searching in REVERSE we will "board" R1 at the first possible stop F and "alight" at the
   * optimal stop B (the best "arrival-time").
   */
  private static final String EXP_PATH_BEST_ARRIVAL_TIME_REVERSE =
    "Walk 10s ~ B ~ BUS R1 0:14 0:38 ~ F ~ Walk 1s [0:13:50 0:38:01 24m11s Tₓ0]";

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder =
    new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  void setup() {
    data.withRoute(
      route("R1", STOP_A, STOP_B, STOP_C, STOP_D, STOP_E, STOP_F).withTimetable(
        schedule("0:10 0:14 0:18 0:30 0:34 0:38")
      )
    );

    requestBuilder
      .searchParams()
      .addAccessPaths(
        TestAccessEgress.walk(STOP_A, D1s),
        TestAccessEgress.walk(STOP_B, D10s), // Best option
        TestAccessEgress.walk(STOP_C, D5m)
      )
      .addEgressPaths(
        TestAccessEgress.walk(STOP_D, D5m),
        TestAccessEgress.walk(STOP_E, D10s), // Best option
        TestAccessEgress.walk(STOP_F, D1s)
      )
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T01_00)
      .searchOneIterationOnly();
    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  static List<RaptorModuleTestCase> testCases() {
    var expected =
      "Walk 10s ~ B ~ BUS R1 0:14 0:34 ~ E ~ Walk 10s [0:13:50 0:34:10 20m20s Tₓ0 C₁1_840]";

    return RaptorModuleTestCase.of()
      .addMinDuration("20m20s", TX_0, T00_00, T01_00)
      // A test on the standard profile is included to demonstrate that the min-travel-duration
      // and the standard give different results. The boarding stop is different.
      .add(standard().forwardOnly(), EXP_PATH_BEST_ARRIVAL_TIME)
      // A reverse test on the standard profile is included to demonstrate that the
      // min-travel-duration and the standard give different results when searching
      // in reverse. The alight stop is different.
      .add(standard().reverseOnly(), EXP_PATH_BEST_ARRIVAL_TIME_REVERSE)
      .add(multiCriteria(), expected)
      .build();
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void testRaptor(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }
}
