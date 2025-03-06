package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flex;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flexAndWalk;
import static org.opentripplanner.raptor._data.transit.TestTransfer.transfer;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.multiCriteria;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.standard;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.api.PathUtils;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCase;

/**
 * FEATURE UNDER TEST
 * <p>
 * Raptor should be able to route Access arriving on-board and egress departing on-board connecting
 * to transit by transfers. Access and egress which arrive/depart at/from the same stops by
 * walking should not be possible.
 */
public class F05_OnBoardAccessEgressAndTransfersTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder =
    new RaptorRequestBuilder<>();

  @BeforeEach
  public void setup() {
    data
      .withTransit("R1", "0:10 0:20", STOP_B, STOP_C)
      .withTransfer(STOP_A, transfer(STOP_B, D10s))
      .withTransfer(STOP_C, transfer(STOP_D, D10s));

    requestBuilder
      .searchParams()
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T00_30)
      .searchWindowInSeconds(D10m);

    requestBuilder
      .searchParams()
      .addAccessPaths(flexAndWalk(STOP_A, D2m), flex(STOP_A, D5m))
      .addEgressPaths(flexAndWalk(STOP_D, D2m), flex(STOP_D, D5m));
  }

  static List<RaptorModuleTestCase> testCases() {
    var path =
      "Flex 5m 1x ~ A ~ Walk 10s ~ B ~ BUS R1 0:10 0:20 ~ C ~ Walk 10s ~ D ~ Flex 5m 1x [0:03:50 0:26:10 22m20s Tₓ2 C₁2_560]";

    return RaptorModuleTestCase.of()
      .addMinDuration("22m20s", 2, T00_00, T00_30)
      .add(standard(), PathUtils.withoutCost(path))
      .add(multiCriteria(), path)
      .build();
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void test(RaptorModuleTestCase testCase) {
    var request = testCase.withConfig(requestBuilder);
    var response = raptorService.route(request, data);
    assertEquals(testCase.expected(), pathsToString(response));
  }
}
