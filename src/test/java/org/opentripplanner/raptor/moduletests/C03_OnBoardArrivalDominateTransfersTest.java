package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.walk;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTransfer.transfer;
import static org.opentripplanner.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
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
 * Raptor should be able to route access and transit arriving on-board and egress departing
 * on-board connecting to transit by transfers. Access and egress witch arrive/depart at/from
 * the same stops by walking should not be possible.
 */
public class C03_OnBoardArrivalDominateTransfersTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();

  @BeforeEach
  public void setup() {
    data
      .withRoute(route(pattern("R1", STOP_A, STOP_B)).withTimetable(schedule().times("0:05 0:08")))
      .withRoute(route(pattern("R2", STOP_B, STOP_C)).withTimetable(schedule().times("0:12 0:15")))
      .withTransfer(STOP_B, transfer(STOP_C, D5m));

    requestBuilder
      .searchParams()
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T00_30)
      .searchWindowInSeconds(D10m);

    requestBuilder
      .searchParams()
      .addAccessPaths(walk(STOP_A, D1m))
      .addEgressPaths(walk(STOP_C, D1m));
  }

  static List<RaptorModuleTestCase> testCases() {
    var path =
      "Walk 1m ~ A ~ " +
      "BUS R1 0:05 0:08 ~ B ~ " +
      "BUS R2 0:12 0:15 ~ C ~ " +
      "Walk 1m " +
      "[0:04 0:16 12m 1tx $2040]";

    return RaptorModuleTestCase
      .of()
      .addMinDuration("9m", 1, T00_00, T00_30)
      .add(standard(), PathUtils.withoutCost(path))
      .add(multiCriteria(), path)
      .build();
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void test(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }
}
