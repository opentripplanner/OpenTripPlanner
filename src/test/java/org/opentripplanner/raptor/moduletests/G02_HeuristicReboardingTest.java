package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.framework.time.TimeUtils.hm2time;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.rangeraptor.internalapi.Heuristics;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.raptor.spi.SearchDirection;
import org.opentripplanner.test.support.VariableSource;

public class G02_HeuristicReboardingTest implements RaptorTestConstants {

  // Any big negative number will do, but -1 is a legal value
  private static final int UNREACHED = -9999;
  private static final int[] BEST_TRANSFERS_REVERSE = { UNREACHED, 0, 0, -1, -1 };
  private static final int[] BEST_TRANSFERS_FORWARD = { UNREACHED, -1, -1, 0, 0 };

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorConfig<TestTripSchedule> config = RaptorConfig.defaultConfigForTest();

  @BeforeEach
  public void setup() {
    data.withRoute(
      route(pattern("R1", STOP_A, STOP_B, STOP_C, STOP_D))
        .withTimetable(schedule("00:01, 00:03, 00:05, 00:07"))
    );

    requestBuilder.slackProvider(RaptorSlackProvider.defaultSlackProvider(0, 0, 0));

    requestBuilder
      .searchParams()
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(hm2time(0, 8))
      .searchOneIterationOnly();
  }

  private static final List<Arguments> profiles = List.of(
    Arguments.of(RaptorProfile.MIN_TRAVEL_DURATION, SearchDirection.FORWARD),
    Arguments.of(RaptorProfile.MIN_TRAVEL_DURATION_BEST_TIME, SearchDirection.FORWARD),
    Arguments.of(RaptorProfile.MIN_TRAVEL_DURATION_AND_COST_BEST_TIME, SearchDirection.FORWARD),
    Arguments.of(RaptorProfile.MIN_TRAVEL_DURATION, SearchDirection.REVERSE),
    Arguments.of(RaptorProfile.MIN_TRAVEL_DURATION_BEST_TIME, SearchDirection.REVERSE),
    Arguments.of(RaptorProfile.MIN_TRAVEL_DURATION_AND_COST_BEST_TIME, SearchDirection.REVERSE)
  );

  /**
   * <pre>
   * Stops: 0..4
   *
   * Stop on route (stop indexes):
   *   R1:  1 - 2 - 3 - 4
   *
   * Schedule:
   *   R1: 00:01 - 00:03 - 00:05 - 00:07
   *
   * Access (toStop & duration):
   *   1  1min
   *   2  2min
   *
   * Egress (fromStop & duration):
   *   3  2min
   *   4  1min
   *
   * </pre>
   */
  @ParameterizedTest(name = "profile {0}, direction {1}")
  @VariableSource("profiles")
  public void testRequireReboarding(RaptorProfile profile, SearchDirection direction) {
    requestBuilder
      .searchParams()
      .addAccessPaths(TestAccessEgress.walk(STOP_A, D1m))
      .addAccessPaths(TestAccessEgress.walk(STOP_B, D2m))
      .addEgressPaths(TestAccessEgress.walk(STOP_C, D2m))
      .addEgressPaths(TestAccessEgress.walk(STOP_D, D1m));

    var request = requestBuilder.profile(profile).searchDirection(direction).build();

    var search = config.createHeuristicSearch(data, data.multiCriteriaCostCalculator(), request);

    search.route();
    var destinationHeuristics = search.heuristics();

    var bestTransfers = direction == SearchDirection.FORWARD
      ? BEST_TRANSFERS_FORWARD
      : BEST_TRANSFERS_REVERSE;

    var bestTimes = direction == SearchDirection.FORWARD
      ? new int[] {
        UNREACHED,
        //  Access + R1
        60,
        2 * 60,
        2 * 60 + 2 * 60,
        2 * 60 + 4 * 60,
      }
      : new int[] {
        UNREACHED,
        //  Egress + R1
        2 * 60 + 4 * 60,
        2 * 60 + 2 * 60,
        2 * 60,
        60,
      };

    assertHeuristics(destinationHeuristics, bestTransfers, bestTimes);
  }

  /**
   * <pre>
   * Stops: 0..4
   *
   * Stop on route (stop indexes):
   *   R1:  1 - 2 - 3 - 4
   *
   * Schedule:
   *   R1: 00:01 - 00:03 - 00:05 - 00:07
   *
   * Access (toStop & duration):
   *   1  1min
   *   2  5min
   *
   * Egress (fromStop & duration):
   *   3  5min
   *   4  1min
   *
   * </pre>
   */
  @ParameterizedTest(name = "profile {0}, direction {1}")
  @VariableSource("profiles")
  public void testSlowerByReboarding(RaptorProfile profile, SearchDirection direction) {
    requestBuilder
      .searchParams()
      .addAccessPaths(TestAccessEgress.walk(STOP_A, D1m))
      .addAccessPaths(TestAccessEgress.walk(STOP_B, D5m))
      .addEgressPaths(TestAccessEgress.walk(STOP_C, D5m))
      .addEgressPaths(TestAccessEgress.walk(STOP_D, D1m));

    var request = requestBuilder.profile(profile).searchDirection(direction).build();

    var search = config.createHeuristicSearch(data, data.multiCriteriaCostCalculator(), request);

    search.route();
    var destinationHeuristics = search.heuristics();

    var bestTransfers = direction == SearchDirection.FORWARD
      ? BEST_TRANSFERS_FORWARD
      : BEST_TRANSFERS_REVERSE;

    var bestTimes = direction == SearchDirection.FORWARD
      ? new int[] {
        UNREACHED,
        //  Access + R1
        60,
        60 + 2 * 60,
        60 + 4 * 60,
        60 + 6 * 60,
      }
      : new int[] {
        UNREACHED,
        //  Egress + R1
        60 + 6 * 60,
        60 + 4 * 60,
        60 + 2 * 60,
        60,
      };

    assertHeuristics(destinationHeuristics, bestTransfers, bestTimes);
  }

  private static void assertHeuristics(
    Heuristics destinationHeuristics,
    int[] bestTransfers,
    int[] bestTimes
  ) {
    assertNotNull(destinationHeuristics);

    assertArrayLessOrEqual(
      bestTransfers,
      destinationHeuristics.bestNumOfTransfersToIntArray(UNREACHED),
      "best number of transfers"
    );
    assertArrayLessOrEqual(
      bestTimes,
      destinationHeuristics.bestTravelDurationToIntArray(UNREACHED),
      "best times"
    );
  }

  private static void assertArrayLessOrEqual(int[] expected, int[] actual, String arrayName) {
    assertNotNull(actual);
    assertEquals(expected.length, actual.length);
    for (int i = 0; i < expected.length; i++) {
      assertTrue(
        expected[i] >= actual[i],
        String.format(
          "Value %d is greater than %d for index %d in %s",
          actual[i],
          expected[i],
          i,
          arrayName
        )
      );
    }
  }
}
