package org.opentripplanner.transit.raptor.moduletests;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.transit.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.flex;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.flexAndWalk;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.walk;
import static org.opentripplanner.transit.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider.defaultSlackProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.algorithm.raptor.transit.cost.RaptorCostConverter;
import org.opentripplanner.transit.raptor.RaptorService;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.transit.TestTransitData;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.request.RaptorProfile;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.api.request.SearchDirection;
import org.opentripplanner.transit.raptor.rangeraptor.configure.RaptorConfig;

/**
 * FEATURE UNDER TEST
 * <p>
 * With FLEX access Raptor must support access paths with more then one leg.
 * These access paths have more transfers that regular paths, hence should not dominate
 * access walking, but only get accepted when they are better on time and/or cost.
 */
public class B10_FlexAccessTest implements RaptorTestConstants {
  private static final int TRANSFER_SLACK = 60;
  private static final int COST_ONE_STOP = RaptorCostConverter.toRaptorCost(2 * 60);
  private static final int COST_TRANSFER_SLACK = RaptorCostConverter.toRaptorCost(TRANSFER_SLACK);
  private static final int COST_ONE_SEC = RaptorCostConverter.toRaptorCost(1);

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
      RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  public void setup() {
    data.withRoute(
        route("R1", STOP_B, STOP_C, STOP_D, STOP_E, STOP_F)
            .withTimetable(
                schedule("0:10, 0:12, 0:14, 0:16, 0:20")
            )
    );
    requestBuilder.searchParams()
        // All access paths are all pareto-optimal (McRaptor).
        .addAccessPaths(
            // lowest num-of-transfers (0)
            walk(STOP_B, D10m, COST_ONE_STOP + COST_TRANSFER_SLACK),
            // lowest cost
            flexAndWalk(STOP_C, D2m, TWO_RIDES, 2*COST_ONE_STOP - COST_ONE_SEC),
            // latest departure time
            flex(STOP_D, D3m, TWO_RIDES, 3*COST_ONE_STOP),
            // best on combination of transfers and time
            flexAndWalk(STOP_E, D7m, ONE_RIDE, 4*COST_ONE_STOP)
        )
        .addEgressPaths(walk(STOP_F, D1m));

    requestBuilder.searchParams()
        .earliestDepartureTime(T00_00)
        .latestArrivalTime(T00_30);

    // We will test board- and alight-slack in a separate test
    requestBuilder.slackProvider(
        defaultSlackProvider(TRANSFER_SLACK, 0, 0)
    );

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  @Test
  public void standard() {
    requestBuilder.profile(RaptorProfile.STANDARD);

    var response = raptorService.route(requestBuilder.build(), data);

    assertEquals(
        "Flex 3m 2x ~ 4 ~ BUS R1 0:14 0:20 ~ 6 ~ Walk 1m [0:10 0:21 11m]",
        pathsToString(response)
    );
  }

  @Test
  public void standardReverse() {
    requestBuilder
        .profile(RaptorProfile.STANDARD)
        .searchDirection(SearchDirection.REVERSE);

    var response = raptorService.route(requestBuilder.build(), data);

    assertEquals(
        "Flex 3m 2x ~ 4 ~ BUS R1 0:14 0:20 ~ 6 ~ Walk 1m [0:10 0:21 11m]",
        pathsToString(response)
    );
  }

  @Test
  public void multiCriteria() {
    requestBuilder.profile(RaptorProfile.MULTI_CRITERIA);

    var response = raptorService.route(requestBuilder.build(), data);

    assertEquals(""
            + "Flex 3m 2x ~ 4 ~ BUS R1 0:14 0:20 ~ 6 ~ Walk 1m [0:10 0:21 11m $1500.00]\n"  // ldt
            + "Flex 2m 2x ~ 3 ~ BUS R1 0:12 0:20 ~ 6 ~ Walk 1m [0:09 0:21 12m $1499.00]\n" // cost
            + "Flex 7m 1x ~ 5 ~ BUS R1 0:16 0:20 ~ 6 ~ Walk 1m [0:08 0:21 13m $1500.00]\n" // tx+time
            + "Walk 10m ~ 2 ~ BUS R1 0:10 0:20 ~ 6 ~ Walk 1m [0:00 0:21 21m $1500.00]",    // tx
        pathsToString(response)
    );
  }
}
