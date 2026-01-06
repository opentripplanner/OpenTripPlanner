package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.multiCriteria;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.standard;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.api.PathUtils;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorTestFactory;
import org.opentripplanner.raptor.moduletests.support.ModuleTestDebugLogging;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCase;
import org.opentripplanner.raptor.spi.TestSlackProvider;

/**
 * FEATURE UNDER TEST
 * <p>
 * Raptor should return a path if it exists when a transfer is only possible because it is
 * guaranteed/stay-seated. A guaranteed transfer should be possible even if there is zero time to
 * do the transfer, even with a short walk leg of 30s between the stops. In these cases the walk
 * leg and the transfer-slack should be ignored and the connection should be possible.
 */
public class E02_GuaranteedWalkTransferTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = data.requestBuilder();
  private final RaptorService<TestTripSchedule> raptorService = RaptorTestFactory.raptorService();

  /**
   * Schedule
   * <pre>
   * Stop:   A       B       C       D
   *   R1: 00:02 - 00:05
   *                  ~ walk ~
   *   R2:                 00:05 - 00:10
   * </pre>
   * <p>
   * Access(stop 1) and egress(stop 3) is 30s.
   */
  @BeforeEach
  public void setup() {
    data
      .access("Walk 30s ~ A")
      .withTimetables(
        """
        A     B
        0:02  0:05
        --
        C     D
        0:05  0:10
        """
      )
      .egress("D ~ Walk 30s");

    var tripA = data.getRoute(0).getTripSchedule(0);
    var tripB = data.getRoute(1).getTripSchedule(0);

    data.withGuaranteedTransfer(tripA, STOP_B, tripB, STOP_C);
    data.withTransfer(STOP_B, TestTransfer.transfer(STOP_C, 30));
    data.withTransferCost(100);

    // NOTE! No search-window is set.
    requestBuilder
      .searchParams()
      .constrainedTransfers(true)
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T00_30)
      .timetable(true);

    // Make sure the slack have values which prevent a normal transfer.
    // The test scenario have zero seconds to transfer and a 30s walk leg, so any slack will do.
    data.withSlackProvider(new TestSlackProvider(D30s, D20s, D10s));

    ModuleTestDebugLogging.setupDebugLogging(data);
  }

  static List<RaptorModuleTestCase> testCases() {
    var path =
      "Walk 30s ~ A ~ BUS R1 0:02 0:05 ~ B ~ Walk 30s ~ C ~ BUS R2 0:05 0:10 ~ D ~ Walk 30s " +
      "[0:01:10 0:10:40 9m30s Tₙ1 C₁1_260]";
    return RaptorModuleTestCase.of()
      // BUG! 10 minutes is wrong, it should be 9m30s - Raptor may drop optimal paths,
      // because of this!
      .addMinDuration("10m", TX_1, T00_00, T00_30)
      .add(standard(), PathUtils.withoutCost(path))
      .add(multiCriteria(), path)
      .build();
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void testRaptor(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }
}
