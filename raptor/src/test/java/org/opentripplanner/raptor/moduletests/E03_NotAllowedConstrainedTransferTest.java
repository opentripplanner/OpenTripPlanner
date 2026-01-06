package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD_REV;
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
import org.opentripplanner.raptor.configure.RaptorTestFactory;
import org.opentripplanner.raptor.moduletests.support.ModuleTestDebugLogging;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCase;

/**
 * FEATURE UNDER TEST
 * <p>
 * Raptor should NOT return a path with a NOT-ALLOWED transfer, instead it should try to find
 * another option.
 */
public class E03_NotAllowedConstrainedTransferTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = data.requestBuilder();
  private final RaptorService<TestTripSchedule> raptorService = RaptorTestFactory.raptorService();

  /**
   * Schedule: Stop:   1       2       3 R1: 00:02 - 00:05 R2:         00:05 - 00:10
   * <p>
   * Access(stop 1) and egress(stop 3) is 30s.
   */
  @BeforeEach
  public void setup() {
    data
      .access("Walk 30s ~ A")
      .withTimetables(
        """
        --
        A     B
        0:02  0:05
        --
        B     C
        0:10  0:15
        0:12  0:17
        --
        B     C
        0:15  0:20
        """
      )
      .egress("C ~ Walk 30s");

    var tripR1a = data.getRoute(0).getTripSchedule(0);
    var tripR2a = data.getRoute(1).getTripSchedule(0);

    // Apply not-allowed on the first trip from R1 and R2 - when found this will apply to
    // the second trip in R2 as well. This is of cause not a correct way to implement the
    // transit model, a not-allowed transfer should apply to ALL trips if constraint is passed
    // to raptor.
    data.withConstrainedTransfer(tripR1a, STOP_B, tripR2a, STOP_B, TestTransitData.TX_NOT_ALLOWED);
    data.withTransferCost(100);

    // NOTE! No search-window set
    requestBuilder
      .searchParams()
      .constrainedTransfers(true)
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T00_30)
      .timetable(true);

    ModuleTestDebugLogging.setupDebugLogging(data);
  }

  static List<RaptorModuleTestCase> testCases() {
    var path =
      "Walk 30s ~ A ~ BUS R1 0:02 0:05 ~ B ~ BUS R3 0:15 0:20 ~ C ~ Walk 30s " +
      "[0:01:30 0:20:30 19m Tₙ1 C₁2_500]";
    return RaptorModuleTestCase.of()
      .addMinDuration("9m", TX_1, T00_00, T00_30)
      .add(standard().not(TC_STANDARD_REV), PathUtils.withoutCost(path))
      .add(multiCriteria(), path)
      .build();
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void testRaptor(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }
}
