package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.withoutCost;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.free;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.walk;
import static org.opentripplanner.raptor._data.transit.TestTransfer.transfer;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD_ONE;
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
import org.opentripplanner.raptor.configure.RaptorTestFactory;
import org.opentripplanner.raptor.moduletests.support.ModuleTestDebugLogging;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCase;
import org.opentripplanner.raptor.spi.TestSlackProvider;

/**
 * FEATURE UNDER TEST
 * <p>
 * This test focus on selecting the correct start on the journey, where a trip R1 competes with a
 * flex access to get to stop C. The Flex access arrives at C one minute after Trip R1 + walking,
 * but the flex arrives on-board. This should give the flex an advantage, the ability to transfer to
 * a nearby stop. In this test we manipulate the EGRESS for make the two paths optional. If we can
 * get both paths (start with flex access or trip R1) as optimal results by changing the egress,
 * then we have proven that both these results are kept in stop arrival at stop C.
 * <p>
 * Further this test also makes sure the path is constructed correctly when we have a mix of access,
 * transfer and transit at the same stop, in the same Raptor round. Two walking legs are not allowed
 * after each other.
 * <p>
 * <img src="images/F11.svg" width="548" height="206"/>
 * <p>
 * We use the same data and changes the egress walk leg to cover all cases. The egress walk leg
 * becomes optimal is it is 3 minutes, while the flex is optimal when we set the egress to 10
 * minutes. Trip R2 is faster than trip R1, but must be using the FLEX egress - since it arrive at
 * the stop C by walking.
 * <p>
 * Note! The 'earliest-departure-time' is set to 00:02, and the board and alight slacks are zero.
 */
public class F11_AccessWithRidesMultipleOptimalPathsTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = data.requestBuilder();
  private final RaptorService<TestTripSchedule> raptorService = RaptorTestFactory.raptorService();

  @BeforeEach
  public void setup() {
    data
      .access("Free ~ A", "Flex 11m Rₙ1 ~ C")
      .withTimetables(
        """
        A     B
        0:02  0:10
        --
        C     E
        0:15  0:20
        --
        D     F
        0:16  0:22
        """
      )
      // We will test board- and alight-slack in a separate test
      .withSlackProvider(new TestSlackProvider(D1_m, D0_s, D0_s));

    requestBuilder
      .searchParams()
      .timetable(true)
      .earliestDepartureTime(T00_02)
      .latestArrivalTime(T00_30);

    data.withTransfer(STOP_B, transfer(STOP_C, D2_m)).withTransfer(STOP_C, transfer(STOP_D, D2_m));

    // Set ModuleTestDebugLogging.DEBUG=true to enable debugging output
    ModuleTestDebugLogging.setupDebugLogging(data);
  }

  /**
   * With 3m walk from Stop E to the destination.
   */
  static List<RaptorModuleTestCase> testCase3mWalkAccess() {
    var startFlexAccess = "Flex 11m Rₙ1 ~ C ";
    var endWalkAndR3 = "~ Walk 2m ~ D ~ BUS R3 0:16 0:22 ~ F ";
    var endR2AndWalk = "~ BUS R2 0:15 0:20 ~ E ~ Walk 3m ";

    // Min-Duration 19m - this is
    var flexTransferTransit = startFlexAccess + endWalkAndR3 + "[0:02 0:22 20m Tₙ1 C₁2_580]";
    // Min-Duration 19m
    var flexAndTransit = startFlexAccess + endR2AndWalk + "[0:03 0:23 20m Tₙ1 C₁2_640]";

    return RaptorModuleTestCase.of()
      .withRequest(r -> r.searchParams().addEgressPaths(free(STOP_F), walk(STOP_E, D3_m)))
      .addMinDuration("19m", TX_1, T00_02, T00_30)
      .add(standard().manyIterations(), withoutCost(flexTransferTransit, flexAndTransit))
      .add(TC_STANDARD_ONE, withoutCost(flexTransferTransit))
      .add(TC_STANDARD_REV_ONE, withoutCost(flexAndTransit))
      .add(multiCriteria(), flexTransferTransit, flexAndTransit)
      .build();
  }

  @ParameterizedTest
  @MethodSource("testCase3mWalkAccess")
  void test3mWalkAccess(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }

  /**
   * With 1m walk from Stop E to the destination.
   */
  static List<RaptorModuleTestCase> testCase1mWalkAccess() {
    var startFlexAccess = "Flex 11m Rₙ1 ~ C ";
    var startWalkAndR1 = "A ~ BUS R1 0:02 0:10 ~ B ~ Walk 2m ~ C ";
    var endR2AndWalk = "~ BUS R2 0:15 0:20 ~ E ~ Walk 1m ";
    // Best duration 16m,
    var transitAndTransit = startWalkAndR1 + endR2AndWalk + "[0:02 0:21 19m Tₙ1]";
    // Min-Duration 17m
    var flexAndTransit = startFlexAccess + endR2AndWalk + "[0:03 0:21 18m Tₙ1 C₁2_400]";

    return RaptorModuleTestCase.of()
      .withRequest(r -> r.searchParams().addEgressPaths(free(STOP_F), walk(STOP_E, D1_m)))
      .addMinDuration("17m", TX_1, T00_02, T00_30)
      .add(standard().manyIterations(), withoutCost(flexAndTransit))
      .add(TC_STANDARD_ONE, withoutCost(transitAndTransit))
      .add(TC_STANDARD_REV_ONE, withoutCost(flexAndTransit))
      .add(multiCriteria(), flexAndTransit)
      .build();
  }

  @ParameterizedTest
  @MethodSource("testCase1mWalkAccess")
  void test1mWalkAccess(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }
}
