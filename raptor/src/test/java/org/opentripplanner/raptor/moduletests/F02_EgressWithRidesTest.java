package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.withoutCost;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flex;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flexAndWalk;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.walk;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MIN_DURATION;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MIN_DURATION_REV;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD_REV_ONE;
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
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.moduletests.support.ModuleTestDebugLogging;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCase;
import org.opentripplanner.raptor.spi.DefaultSlackProvider;
import org.opentripplanner.utils.time.DurationUtils;

/**
 * FEATURE UNDER TEST
 * <p>
 * With FLEX access and egress Raptor must support access/egress paths with more then one leg. These
 * access paths have more transfers that regular paths, hence should not dominate access/egress
 * walking, but only get accepted when they are better on time and/or cost.
 */
public class F02_EgressWithRidesTest implements RaptorTestConstants {

  private static final int D1m59s = DurationUtils.durationInSeconds("1m59s");

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
        schedule("0:10, 0:12, 0:14, 0:16, 0:18")
      )
    );
    // We will test board- and alight-slack in a separate test
    data.withSlackProvider(new DefaultSlackProvider(60, 0, 0));

    requestBuilder
      .searchParams()
      .addAccessPaths(walk(STOP_B, D1m))
      // All egress paths are all pareto-optimal (McRaptor).
      .addEgressPaths(
        flexAndWalk(STOP_C, D7m), // best combination of transfers and time
        flex(STOP_D, D3m, TWO_RIDES), // earliest arrival time
        flexAndWalk(STOP_E, D1m59s, TWO_RIDES), // lowest cost
        walk(STOP_F, D10m) // lowest num-of-transfers (0)
      );
    requestBuilder.searchParams().earliestDepartureTime(T00_00).latestArrivalTime(T00_30);

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  static List<RaptorModuleTestCase> testCases() {
    var prefix = "Walk 1m ~ B ~ BUS R1 0:10 ";
    var bestArrivalTime = prefix + "0:14 ~ D ~ Flex 3m 2x [0:09 0:18 9m Tₓ2 C₁1_380]";
    var bestNTransfers = prefix + "0:18 ~ F ~ Walk 10m [0:09 0:28 19m Tₓ0 C₁2_400]";
    var bestCost = prefix + "0:16 ~ E ~ Flex+Walk 1m59s 2x [0:09 0:18:59 9m59s Tₓ2 C₁1_378]";
    var bestTxAndTime = prefix + "0:12 ~ C ~ Flex+Walk 7m 1x [0:09 0:20 11m Tₓ1 C₁1_740]";

    return RaptorModuleTestCase.of()
      .add(TC_MIN_DURATION, "[0:00 0:09 9m Tₓ2]", "[0:00 0:11 11m Tₓ1]", "[0:00 0:19 19m Tₓ0]")
      .add(TC_MIN_DURATION_REV, "[0:21 0:30 9m Tₓ0]")
      .add(standard().not(TC_STANDARD_REV_ONE), withoutCost(bestArrivalTime))
      // "First" alighting wins for reverse
      .add(TC_STANDARD_REV_ONE, withoutCost(bestNTransfers))
      .add(multiCriteria(), bestArrivalTime, bestCost, bestTxAndTime, bestNTransfers)
      .build();
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void testRaptor(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }
}
