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
import org.opentripplanner.raptor._data.transit.TestTransferConstraint;
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
 * guaranteed/stay-seated. A stay-seated transfer should be possible even if there is zero time to
 * do the transfer. In these cases the transfer-slack should be ignored and the connection should
 * be possible.
 */
public class E01_StaySeatedTransferTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = data.requestBuilder();
  private final RaptorService<TestTripSchedule> raptorService = RaptorTestFactory.raptorService();

  @BeforeEach
  public void setup() {
    data
      .access("Walk 30s ~ A")
      .withTimetables(
        """
        A     B
        0:02  0:05
        --
        B     C
        0:05  0:10
        """
      )
      .egress("C ~ Walk 30s");

    // No slack for transfer at stop B
    var tripA = data.getRoute(0).getTripSchedule(0);
    var tripB = data.getRoute(1).getTripSchedule(0);

    data.withConstrainedTransfer(tripA, STOP_B, tripB, STOP_B, TestTransferConstraint.staySeated());
    data.withTransferCost(100);

    // NOTE! No search-window is set.
    requestBuilder
      .searchParams()
      .constrainedTransfers(true)
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T00_30)
      .timetable(true);

    // Make sure the slack have values which prevent a normal transfer.
    // The test scenario have zero seconds to transfer, so any slack will do.
    data.withSlackProvider(new TestSlackProvider(D30_s, D20_s, D10_s));

    ModuleTestDebugLogging.setupDebugLogging(data);
  }

  static List<RaptorModuleTestCase> testCases() {
    // Note! The number of transfers is zero with stay-seated/interlining
    var path =
      "Walk 30s ~ A ~ BUS R1 0:02 0:05 ~ B ~ BUS R2 0:05 0:10 ~ C ~ Walk 30s " +
      "[0:01:10 0:10:40 9m30s Tₙ0 C₁1_230]";
    return RaptorModuleTestCase.of()
      .addMinDuration("9m30s", TX_1, T00_00, T00_30)
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
