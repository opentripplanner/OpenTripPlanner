package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.RaptorTestConstants.D30s;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_A;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_B;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_C;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_D;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_E;
import static org.opentripplanner.raptor._data.RaptorTestConstants.T00_00;
import static org.opentripplanner.raptor._data.RaptorTestConstants.T01_00;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MULTI_CRITERIA;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCase;

public class J01_ViaPassThroughTest {

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
    // TODO: 2023-05-16 via pass through: add test case without walk egress
    var r1 = route("R1", STOP_A, STOP_B, STOP_E).withTimetable(schedule("0:02 0:05 0:20"));
    var r2 = route("R2", STOP_A, STOP_C, STOP_D).withTimetable(schedule("0:02 0:10 0:50"));

    // A - B - D - 20 mins
    // A - C - D - 120 mins

    // via: C

    var tripA = r1.timetable().getTripSchedule(0);
    var tripB = r2.timetable().getTripSchedule(0);

    data.withRoutes(r1, r2);
    //    data.withGuaranteedTransfer(tripA, STOP_B, tripB, STOP_B);
    data.mcCostParamsBuilder().transferCost(100);

    // NOTE! No search-window is set.
    requestBuilder
      .searchParams()
      //      .constrainedTransfers(true)
      .addAccessPaths(TestAccessEgress.walk(STOP_A, D30s))
      .addEgressPaths(TestAccessEgress.walk(STOP_D, D30s), TestAccessEgress.walk(STOP_E, D30s))
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T01_00)
      .searchWindow(Duration.ofMinutes(2))
      .timetable(true);

    // Make sure the slack have values which prevent the normal from happening transfer.
    // The test scenario have zero seconds to transfer, so any slack will do.
    //    data.withSlackProvider(new DefaultSlackProvider(D30s, D20s, D10s));

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  static List<RaptorModuleTestCase> testCases() {
    var path =
      "Walk 30s ~ A ~ BUS R1 0:02 0:20 ~ E ~ Walk 30s [0:01:30 0:20:30 19m 0tx $1800]\n" +
      "Walk 30s ~ A ~ BUS R2 0:02 0:50 ~ D ~ Walk 30s [0:01:30 0:50:30 49m 0tx $3600]";
    return RaptorModuleTestCase
      .of()
      //      .addMinDuration("9m30s", TX_1, T00_00, T00_30)
      //      .add(standard(), PathUtils.withoutCost(path))
      .add(TC_MULTI_CRITERIA, path)
      .build();
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void testRaptor(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }
}
