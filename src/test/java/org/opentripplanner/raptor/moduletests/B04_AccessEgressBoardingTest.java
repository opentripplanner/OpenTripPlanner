package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.api.request.RaptorProfile.MIN_TRAVEL_DURATION;
import static org.opentripplanner.raptor.api.request.RaptorProfile.MULTI_CRITERIA;
import static org.opentripplanner.raptor.api.request.RaptorProfile.STANDARD;
import static org.opentripplanner.raptor.spi.SearchDirection.REVERSE;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.api.PathUtils;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;

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
    "Walk 1s ~ A ~ BUS R1 0:10 0:34 ~ E ~ Walk 10s [0:09:59 0:34:10 24m11s 0tx]";

  /**
   * Searching in REVERSE we will "board" R1 at the first possible stop F and "alight" at the
   * optimal stop B (the best "arrival-time").
   */
  private static final String EXP_PATH_BEST_ARRIVAL_TIME_REVERSE =
    "Walk 10s ~ B ~ BUS R1 0:14 0:38 ~ F ~ Walk 1s [0:13:50 0:38:01 24m11s 0tx]";

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  /** Board R1 at stop B and alight at stop E */
  private final String OPTIMAL_PATH =
    "Walk 10s ~ B ~ BUS R1 0:14 0:34 ~ E ~ Walk 10s [0:13:50 0:34:10 20m20s 0tx";

  /** Expect the optimal path to be found. */
  private final String EXP_PATH_MIN_TRAVEL_DURATION = OPTIMAL_PATH + "]";

  /**
   * The multi-criteria search should find the best alternative, because it looks at the
   * arrival-time, generalized-cost, and departure-time(travel-time). In this case the same path as
   * the min-travel-duration will find.
   */
  private final String EXP_PATH_MC = OPTIMAL_PATH + " $1840]";

  @BeforeEach
  public void setup() {
    data.withRoute(
      route("R1", STOP_A, STOP_B, STOP_C, STOP_D, STOP_E, STOP_F)
        .withTimetable(schedule("0:10 0:14 0:18 0:30 0:34 0:38"))
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

  /**
   * A test on the standard profile is included to demonstrate that the min-travel-duration and the
   * standard give different results. The boarding stop is different.
   */
  @Test
  public void standard() {
    requestBuilder.profile(STANDARD);
    var response = raptorService.route(requestBuilder.build(), data);
    assertEquals(EXP_PATH_BEST_ARRIVAL_TIME, PathUtils.pathsToString(response));
  }

  @Test
  public void minTravelDuration() {
    requestBuilder.profile(MIN_TRAVEL_DURATION);
    var response = raptorService.route(requestBuilder.build(), data);
    assertEquals(EXP_PATH_MIN_TRAVEL_DURATION, PathUtils.pathsToString(response));
  }

  /**
   * A reverse test on the standard profile is included to demonstrate that the min-travel-duration
   * and the standard give different results when searching in reverse. The alight stop is
   * different.
   */
  @Test
  public void standardReverse() {
    requestBuilder.profile(STANDARD).searchDirection(REVERSE);
    var response = raptorService.route(requestBuilder.build(), data);
    assertEquals(EXP_PATH_BEST_ARRIVAL_TIME_REVERSE, PathUtils.pathsToString(response));
  }

  @Test
  public void minTravelDurationReverse() {
    requestBuilder.profile(MIN_TRAVEL_DURATION).searchDirection(REVERSE);
    var response = raptorService.route(requestBuilder.build(), data);
    assertEquals(EXP_PATH_MIN_TRAVEL_DURATION, PathUtils.pathsToString(response));
  }

  @Test
  public void multiCriteria() {
    requestBuilder.profile(MULTI_CRITERIA);
    var response = raptorService.route(requestBuilder.build(), data);
    assertEquals(EXP_PATH_MC, PathUtils.pathsToString(response));
  }
}
