package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_B;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_C;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_D;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_E;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_F;
import static org.opentripplanner.raptor._data.RaptorTestConstants.T00_00;
import static org.opentripplanner.raptor._data.RaptorTestConstants.T01_00;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.walk;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.moduletests.support.TestGroupPriorityCalculator.GROUP_A;
import static org.opentripplanner.raptor.moduletests.support.TestGroupPriorityCalculator.GROUP_B;
import static org.opentripplanner.raptor.moduletests.support.TestGroupPriorityCalculator.GROUP_C;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.api.request.RaptorTransitGroupPriorityCalculator;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.moduletests.support.TestGroupPriorityCalculator;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.RaptorCostConverter;

/**
 * FEATURE UNDER TEST
 *
 * Raptor should be able to handle route request with transit-priority.
 */
public class K02_TransitPriorityDestinationTest {

  private static final RaptorTransitGroupPriorityCalculator PRIORITY_GROUP_CALCULATOR =
    TestGroupPriorityCalculator.PRIORITY_CALCULATOR;
  private static final int C1_SLACK_90s = RaptorCostConverter.toRaptorCost(90);

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  private void prepareRequest() {
    // Each pattern depart at the same time, but arrive at different times and they may
    // belong to different groups.
    // Line U1 is not optimal, because it slower than L1 and is in the same group.
    // Given a slack on the cost equals to ~90s this makes both L1 and L2 optimal (since
    // they are in different groups), but not L3 (which certainly is in its own group but
    // its cost is outside the range allowed by the slack).
    data.withRoutes(
      route(pattern("L1", STOP_B, STOP_C).withPriorityGroup(GROUP_A))
        .withTimetable(schedule("00:02 00:12")),
      route(pattern("U1", STOP_B, STOP_D).withPriorityGroup(GROUP_A))
        .withTimetable(schedule("00:02 00:12:01")),
      route(pattern("L2", STOP_B, STOP_E).withPriorityGroup(GROUP_B))
        .withTimetable(schedule("00:02 00:13")),
      route(pattern("L3", STOP_B, STOP_F).withPriorityGroup(GROUP_C))
        .withTimetable(schedule("00:02 00:14"))
    );

    requestBuilder
      .profile(RaptorProfile.MULTI_CRITERIA)
      // TODO: 2023-07-24 Currently heuristics does not work with pass-through so we
      //  have to turn them off. Make sure to re-enable optimization later when it's fixed
      .clearOptimizations();

    requestBuilder
      .searchParams()
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T01_00)
      .searchWindow(Duration.ofMinutes(2))
      .timetable(true);

    requestBuilder.withMultiCriteria(mc ->
      // Raptor cost 9000 ~= 90 seconds slack
      mc
        .withRelaxC1(value -> value + C1_SLACK_90s)
        .withTransitPriorityCalculator(PRIORITY_GROUP_CALCULATOR)
    );
    // Add 1 second access/egress paths
    requestBuilder
      .searchParams()
      .addAccessPaths(walk(STOP_B, 1))
      .addEgressPaths(walk(STOP_C, 1))
      .addEgressPaths(walk(STOP_D, 1))
      .addEgressPaths(walk(STOP_E, 1))
      .addEgressPaths(walk(STOP_F, 1));
  }

  @Test
  public void transitPriority() {
    // We expect L1 & L2 but not L3, since the cost of L3 is > $90.00.
    assertEquals(
      """
      Walk 1s ~ B ~ BUS L1 0:02 0:12 ~ C ~ Walk 1s [0:01:59 0:12:01 10m2s Tₓ0 C₁1_204 C₂1]
      Walk 1s ~ B ~ BUS L2 0:02 0:13 ~ E ~ Walk 1s [0:01:59 0:13:01 11m2s Tₓ0 C₁1_264 C₂2]
      """.trim(),
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }
}
