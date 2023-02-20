package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
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
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCase;
import org.opentripplanner.raptor.spi.DefaultSlackProvider;

/**
 * FEATURE UNDER TEST
 * <p>
 * Raptor should return a path if it exists when a transfer is only possible because it is
 * guaranteed/stay-seated. A guarantied transfer should be able even if there is zero time to do the
 * transfer. In these cases the transfer-slack should be ignored and the connection should be
 * possible.
 */
public class E01_GuaranteedTransferTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  /**
   * Schedule: Stop:   1       2       3 R1: 00:02 - 00:05 R2:         00:05 - 00:10
   * <p>
   * Access(stop 1) and egress(stop 3) is 30s.
   */
  @BeforeEach
  public void setup() {
    var r1 = route("R1", STOP_A, STOP_B).withTimetable(schedule("0:02 0:05"));
    var r2 = route("R2", STOP_B, STOP_C).withTimetable(schedule("0:05 0:10"));

    var tripA = r1.timetable().getTripSchedule(0);
    var tripB = r2.timetable().getTripSchedule(0);

    data.withRoutes(r1, r2);
    data.withGuaranteedTransfer(tripA, STOP_B, tripB, STOP_B);
    data.mcCostParamsBuilder().transferCost(100);

    // NOTE! No search-window is set.
    requestBuilder
      .searchParams()
      .constrainedTransfers(true)
      .addAccessPaths(TestAccessEgress.walk(STOP_A, D30s))
      .addEgressPaths(TestAccessEgress.walk(STOP_C, D30s))
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T00_30)
      .timetable(true);

    // Make sure the slack have values which prevent the normal from happening transfer.
    // The test scenario have zero seconds to transfer, so any slack will do.
    data.withSlackProvider(new DefaultSlackProvider(D30s, D20s, D10s));

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  static List<RaptorModuleTestCase> testCases() {
    var path =
      "Walk 30s ~ A ~ BUS R1 0:02 0:05 ~ B ~ BUS R2 0:05 0:10 ~ C ~ Walk 30s " +
      "[0:01:10 0:10:40 9m30s 1tx $1230]";
    return RaptorModuleTestCase
      .of()
      // TODO - board-slack is added for FORWARD search, while alight-slack is added for
      //      - REVERSE. We should add both, but that requires a bit of refactoring and
      //      - providing constraint-transfer information at the place where alight-slack
      //      - should be added in Raptor.
      .add(TC_MIN_DURATION, "[0:00 0:09:20 9m20s 1tx]")
      .add(TC_MIN_DURATION_REV, "[0:20:50 0:30 9m10s 1tx]")
      .add(standard(), PathUtils.withoutCost(path))
      .add(multiCriteria(), path)
      .build();
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void testRaptor(RaptorModuleTestCase testCase) {
    var request = testCase.withConfig(requestBuilder);
    var response = raptorService.route(request, data);
    assertEquals(testCase.expected(), pathsToString(response));
  }
}
