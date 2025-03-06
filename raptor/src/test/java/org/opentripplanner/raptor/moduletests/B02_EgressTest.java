package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD_REV_ONE;
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
 * Raptor should return the optimal path with various egress paths. All Raptor
 * optimizations(McRaptor, Standard and Reverse Standard) should be tested.
 */
public class B02_EgressTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder =
    new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  void setup() {
    data.withRoute(
      route("R1", STOP_B, STOP_C, STOP_D, STOP_E, STOP_F, STOP_G).withTimetable(
        schedule("0:10, 0:14, 0:18, 0:20, 0:24, 0:28")
      )
    );

    requestBuilder
      .searchParams()
      .addAccessPaths(TestAccessEgress.walk(STOP_B, D20s))
      .addEgressPaths(
        TestAccessEgress.walk(STOP_D, D20m), // Not optimal
        TestAccessEgress.walk(STOP_E, D7m), // Earliest arrival time: 0:16 + 3m = 0:19
        TestAccessEgress.walk(STOP_F, D4m), // Best compromise of cost and time
        TestAccessEgress.walk(STOP_G, D1s) // Lowest cost
      )
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T00_30);

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  static List<RaptorModuleTestCase> testCases() {
    String expStd = "Walk 20s ~ B ~ BUS R1 0:10 0:20 ~ E ~ Walk 7m [0:09:40 0:27 17m20s Tₓ0]";
    String expStdRevOne =
      "Walk 20s ~ B ~ BUS R1 0:10 0:28 ~ G ~ Walk 1s [0:09:40 0:28:01 18m21s Tₓ0]";
    return RaptorModuleTestCase.of()
      .addMinDuration("17m20s", TX_0, T00_00, T00_30)
      .add(standard().not(TC_STANDARD_REV_ONE), expStd)
      // When we run one iteration the egress "alighting" last is used as long as it
      // arrives at the origin at the same time. In this case the "worst" path is kept.
      .add(TC_STANDARD_REV_ONE, expStdRevOne)
      .add(
        multiCriteria(),
        "Walk 20s ~ B ~ BUS R1 0:10 0:20 ~ E ~ Walk 7m [0:09:40 0:27 17m20s Tₓ0 C₁2_080]",
        "Walk 20s ~ B ~ BUS R1 0:10 0:24 ~ F ~ Walk 4m [0:09:40 0:28 18m20s Tₓ0 C₁1_960]",
        "Walk 20s ~ B ~ BUS R1 0:10 0:28 ~ G ~ Walk 1s [0:09:40 0:28:01 18m21s Tₓ0 C₁1_722]"
      )
      .build();
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void testRaptor(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }
}
