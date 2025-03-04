package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.walk;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTransfer.transfer;
import static org.opentripplanner.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MIN_DURATION;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MIN_DURATION_REV;
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
import org.opentripplanner.raptor.moduletests.support.ModuleTestDebugLogging;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCase;

/**
 * FEATURE UNDER TEST
 * <p>
 * Raptor should be able to route access and transit arriving on-board and egress departing
 * on-board connecting to transit by transfers. Access and egress which arrive/depart at/from
 * the same stops by walking should not be possible.
 */
public class C03_OnBoardArrivalDominateTransfersTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder =
    new RaptorRequestBuilder<>();

  @BeforeEach
  public void setup() {
    data
      .withRoute(route(pattern("R1", STOP_A, STOP_B)).withTimetable(schedule().times("0:05 0:08")))
      .withRoute(route(pattern("R2", STOP_B, STOP_C)).withTimetable(schedule().times("0:12 0:15")))
      // We add a transfer here which arrive at C before R2, but it should not be used.
      .withTransfer(STOP_B, transfer(STOP_C, D1m));

    requestBuilder
      .searchParams()
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T00_30)
      .searchWindowInSeconds(D10m);

    requestBuilder
      .searchParams()
      .addAccessPaths(walk(STOP_A, D1m))
      .addEgressPaths(walk(STOP_C, D1m));

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  static List<RaptorModuleTestCase> testCases() {
    var path =
      "Walk 1m ~ A ~ " +
      "BUS R1 0:05 0:08 ~ B ~ " +
      "BUS R2 0:12 0:15 ~ C ~ " +
      "Walk 1m " +
      "[0:04 0:16 12m Tₓ1 C₁2_040]";

    return RaptorModuleTestCase.of()
      // Zero transfers is wrong, it is caused by the egress stop(C) being reached by transfer
      // with 0tx(before boarding), so 0 transfers is stored in BestNumberOfTransfers for stop C.
      // Then since the path is computed after, not during, the Raptor search this fails combine
      // the egress with the correct path. The result is still valid to use as heuristics, so it
      // does not cause the mc-raptor w/destination pruning to miss results. There are two ways to
      // fix this. We can keep the best-number-of-transfers for both transit and over-all for all
      // stops (at least egress stops) or we can compute paths during the search. The last
      // solution is probably the one which give the best performance, but the first will make
      // mc-raptor perform better (tighter heuristics).
      .add(TC_MIN_DURATION, "[0:00 0:09 9m Tₓ0]")
      .add(TC_MIN_DURATION_REV, "[0:21 0:30 9m Tₓ1]")
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
