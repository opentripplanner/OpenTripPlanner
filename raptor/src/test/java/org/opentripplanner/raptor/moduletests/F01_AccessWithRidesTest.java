package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flex;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flexAndWalk;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.walk;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MIN_DURATION;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MIN_DURATION_REV;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD_ONE;
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
import org.opentripplanner.raptor.api.model.RaptorCostConverter;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.moduletests.support.ModuleTestDebugLogging;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCase;
import org.opentripplanner.raptor.spi.DefaultSlackProvider;

/**
 * FEATURE UNDER TEST
 * <p>
 * With FLEX access Raptor must support access paths with more than one leg. These access paths have
 * more transfers that regular paths, hence should not dominate access walking, but only get
 * accepted when they are better on time and/or cost.
 */
public class F01_AccessWithRidesTest implements RaptorTestConstants {

  private static final int TRANSFER_SLACK = 60;
  private static final int C1_ONE_STOP = RaptorCostConverter.toRaptorCost(2 * 60);
  private static final int C1_TRANSFER_SLACK = RaptorCostConverter.toRaptorCost(TRANSFER_SLACK);
  private static final int C1_ONE_SEC = RaptorCostConverter.toRaptorCost(1);

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder =
    new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  void setup() {
    data.withRoute(
      route("R1", STOP_B, STOP_C, STOP_D, STOP_E, STOP_F).withTimetable(
        schedule("0:10, 0:12, 0:14, 0:16, 0:20")
      )
    );
    // We will test board- and alight-slack in a separate test
    data.withSlackProvider(new DefaultSlackProvider(TRANSFER_SLACK, 0, 0));

    requestBuilder
      .searchParams()
      // All access paths are all pareto-optimal (McRaptor).
      .addAccessPaths(
        // lowest num-of-transfers (0)
        walk(STOP_B, D10m, C1_ONE_STOP + C1_TRANSFER_SLACK),
        // lowest cost
        flexAndWalk(STOP_C, D2m, TWO_RIDES, 2 * C1_ONE_STOP - C1_ONE_SEC),
        // latest departure time
        flex(STOP_D, D3m, TWO_RIDES, 3 * C1_ONE_STOP),
        // best on combination of transfers and time
        flexAndWalk(STOP_E, D7m, ONE_RIDE, 4 * C1_ONE_STOP)
      )
      .addEgressPaths(walk(STOP_F, D1m));

    requestBuilder.searchParams().earliestDepartureTime(T00_00).latestArrivalTime(T00_30);

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  static List<RaptorModuleTestCase> testCases() {
    String expFlexAccess = "Flex 3m 2x ~ D ~ BUS R1 0:14 0:20 ~ F ~ Walk 1m [0:10 0:21 11m Tₓ2]";
    String expWalkAccess = "Walk 10m ~ B ~ BUS R1 0:10 0:20 ~ F ~ Walk 1m [0:00 0:21 21m Tₓ0]";
    return RaptorModuleTestCase.of()
      // TODO - Why do we get only one result here - when there is 3 different pareto-optimal
      //      - paths
      .add(TC_MIN_DURATION, "[0:00 0:11 11m Tₓ0]")
      // Return pareto optimal paths with 0, 1 and 2 num-of-transfers
      .add(TC_MIN_DURATION_REV, "[0:19 0:30 11m Tₓ2]", "[0:17 0:30 13m Tₓ1]", "[0:09 0:30 21m Tₓ0]")
      .add(standard().not(TC_STANDARD_ONE), expFlexAccess)
      // First boarding wins with one-iteration (apply to min-duration and std-one)
      .add(TC_STANDARD_ONE, expWalkAccess)
      .add(
        multiCriteria(),
        "Flex 3m 2x ~ D ~ BUS R1 0:14 0:20 ~ F ~ Walk 1m [0:10 0:21 11m Tₓ2 C₁1_500]", // ldt
        "Flex+Walk 2m 2x ~ C ~ BUS R1 0:12 0:20 ~ F ~ Walk 1m [0:09 0:21 12m Tₓ2 C₁1_499]", // cost
        "Flex+Walk 7m 1x ~ E ~ BUS R1 0:16 0:20 ~ F ~ Walk 1m [0:08 0:21 13m Tₓ1 C₁1_500]", // tx+time
        "Walk 10m ~ B ~ BUS R1 0:10 0:20 ~ F ~ Walk 1m [0:00 0:21 21m Tₓ0 C₁1_500]" // tx
      )
      .build();
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void testRaptor(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }
}
