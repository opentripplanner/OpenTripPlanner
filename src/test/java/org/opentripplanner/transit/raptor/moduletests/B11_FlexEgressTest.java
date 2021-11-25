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
import org.opentripplanner.transit.raptor.RaptorService;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.transit.TestTransitData;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.request.RaptorProfile;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.api.request.SearchDirection;
import org.opentripplanner.transit.raptor.rangeraptor.configure.RaptorConfig;
import org.opentripplanner.util.time.DurationUtils;

/**
 * FEATURE UNDER TEST
 * <p>
 * With FLEX access and egress Raptor must support access/egress paths with more then one leg.
 * These access paths have more transfers that regular paths, hence should not dominate
 * access/egress walking, but only get accepted when they are better on time and/or cost.
 */
public class B11_FlexEgressTest implements RaptorTestConstants {

  private static final int D1m59s = DurationUtils.duration("1m59s");

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder =
          new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
          RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  public void setup() {
    data.withRoute(
            route("R1", STOP_B, STOP_C, STOP_D, STOP_E, STOP_F)
                    .withTimetable(
                            schedule("0:10, 0:12, 0:14, 0:16, 0:18")
                    )
    );
    requestBuilder.searchParams()
            .addAccessPaths(
                    walk(STOP_B, D1m)
            )
            // All egress paths are all pareto-optimal (McRaptor).
            .addEgressPaths(
                    flexAndWalk(STOP_C, D7m),              // best combination of transfers and time
                    flex(STOP_D, D3m, TWO_RIDES),          // earliest arrival time
                    flexAndWalk(STOP_E, D1m59s, TWO_RIDES), // lowest cost
                    walk(STOP_F, D10m)                     // lowest num-of-transfers (0)
            );
    requestBuilder.searchParams()
            .earliestDepartureTime(T00_00)
            .latestArrivalTime(T00_30);

    // We will test board- and alight-slack in a separate test
    requestBuilder.slackProvider(
            defaultSlackProvider(60, 0, 0)
    );

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  @Test
  public void standard() {
    requestBuilder.profile(RaptorProfile.STANDARD);

    var response = raptorService.route(requestBuilder.build(), data);

    assertEquals(
        "Walk 1m ~ B ~ BUS R1 0:10 0:14 ~ D ~ Flex 3m 2x [0:09 0:18 9m]",
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
        "Walk 1m ~ B ~ BUS R1 0:10 0:14 ~ D ~ Flex 3m 2x [0:09 0:18 9m]",
            pathsToString(response)
    );
  }

  @Test
  public void multiCriteria() {
    requestBuilder.profile(RaptorProfile.MULTI_CRITERIA);

    var response = raptorService.route(requestBuilder.build(), data);

    assertEquals(""
            + "Walk 1m ~ B ~ BUS R1 0:10 0:14 ~ D ~ Flex 3m 2x [0:09 0:18 9m $1380]\n"
            + "Walk 1m ~ B ~ BUS R1 0:10 0:16 ~ E ~ Flex 1m59s 2x [0:09 0:18:59 9m59s $1378]\n"
            + "Walk 1m ~ B ~ BUS R1 0:10 0:12 ~ C ~ Flex 7m 1x [0:09 0:20 11m $1740]\n"
            + "Walk 1m ~ B ~ BUS R1 0:10 0:18 ~ F ~ Walk 10m [0:09 0:28 19m $2400]",
            pathsToString(response)
    );
  }
}