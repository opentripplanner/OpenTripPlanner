package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flex;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.walk;
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
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorTestFactory;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCase;

/*
 * FEATURE UNDER TEST
 *
 * A time-penalty on the access is part of the arrival time of a journey without transit (flex
 * only), because it is added to the access duration. This is the intended behavior: the penalty
 * makes the flex less attractive when comparing it with regular transit. A transit journey that
 * arrives before the flex arrival-time including the penalty dominates the flex-only journey,
 * even if it arrives after the real flex arrival-time. If the transit journey is slower than
 * that, both journeys are optimal.
 *
 * The flex-only journey: Flex 7m (penalty 10m) ~ B ~ Walk 1m. It arrives at 0:18 if it departs
 * at 0:10, but the arrival-time used for comparison is 0:28.
 */
public class L02_TimePenaltyAccessNoTransitTest implements RaptorTestConstants {

  // The flex is more expensive than the bus, so the time-penalty decides if it is dominated
  private static final int FLEX_COST = 500_000;

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = data.requestBuilder();
  private final RaptorService<TestTripSchedule> raptorService = RaptorTestFactory.raptorService();

  @BeforeEach
  public void setup() {
    requestBuilder
      .searchParams()
      .addAccessPaths(walk(STOP_A, D2_m), flex(STOP_B, D7_m, 1, FLEX_COST).withTimePenalty(D10_m))
      .addEgressPaths(walk(STOP_B, D1_m))
      .earliestDepartureTime(T00_10)
      .latestArrivalTime(T01_00)
      .searchWindowInSeconds(D1_m);
  }

  static List<RaptorModuleTestCase> transitArrivesBeforeFlexWithPenaltyTestCases() {
    var bus = "Walk 2m ~ A ~ BUS R1 0:12 0:24 ~ B ~ Walk 1m [0:10 0:25 15m Tₙ0";
    return RaptorModuleTestCase.of()
      .add(standard().forwardOnly(), bus + "]")
      .add(multiCriteria(), bus + " C₁1_680]")
      .build();
  }

  @ParameterizedTest
  @MethodSource("transitArrivesBeforeFlexWithPenaltyTestCases")
  void transitArrivesBeforeFlexWithPenalty(RaptorModuleTestCase testCase) {
    // Arrives at 0:25, after the real flex arrival at 0:18, but before 0:28 (with penalty)
    data.withRoute(route("R1", STOP_A, STOP_B).withTimetable(schedule("0:12 0:24")));
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }

  static List<RaptorModuleTestCase> transitArrivesAfterFlexWithPenaltyTestCases() {
    var flex = "Flex 7m Rₙ1 Pₜ600 ~ B ~ Walk 1m [0:10 0:18 8m Tₙ0";
    var bus = "Walk 2m ~ A ~ BUS R1 0:12 0:34 ~ B ~ Walk 1m [0:10 0:35 25m Tₙ0";
    return RaptorModuleTestCase.of()
      .add(standard().forwardOnly(), flex + "]")
      // The bus is cheaper, so it is optimal on cost, and the flex is optimal on time
      .add(multiCriteria(), flex + " C₁5_120]\n" + bus + " C₁2_280]")
      .build();
  }

  @ParameterizedTest
  @MethodSource("transitArrivesAfterFlexWithPenaltyTestCases")
  void transitArrivesAfterFlexWithPenalty(RaptorModuleTestCase testCase) {
    // Arrives at 0:35, after the flex arrival including the penalty at 0:28
    data.withRoute(route("R1", STOP_A, STOP_B).withTimetable(schedule("0:12 0:34")));
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }
}
