package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCaseFactory;

/**
 * FEATURE UNDER TEST
 * <p>
 * Raptor should return the optimal path with various access and egress paths. The Standard and
 * Standard Reverse optimizations should return the best time option, while the Multi-criteria
 * optimization should return a pareto optimal solutions with/without timetable view enabled.
 */
public class B03_AccessEgressTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = data.requestBuilder();
  private final RaptorService<TestTripSchedule> raptorService = RaptorTestFactory.raptorService();

  @BeforeEach
  public void setup() {
    data
      .access(
        // Lowest cost
        "Walk 1s ~ A",
        // Best compromise of cost and time
        "Walk 4m ~ B",
        // Latest departure time: 0:16 - 5m = 0:11
        "Walk 7m ~ C",
        // Not optimal
        "Walk 20m ~ D"
      )
      .withTimetables(
        """
          A     B     C     D     E     F     G     H
          0:10  0:14  0:18  0:22  0:28  0:32  0:36  0:40
        """
      )
      .egress(
        // Not optimal
        "E ~ Walk 20m",
        // Earliest arrival time: 0:18 + 3m = 0:21
        "F ~ Walk 7m",
        // Best compromise of cost and time
        "G ~ Walk 4m",
        // Lowest cost
        "H ~ Walk 1s"
      );

    requestBuilder.searchParams().earliestDepartureTime(T00_00).latestArrivalTime(T01_00);

    ModuleTestDebugLogging.setupDebugLogging(data);
  }

  /** Only the multi-criteria test-cases differ for timetableView on/off */
  private static RaptorModuleTestCaseFactory standardTestCases() {
    String expStd = "Walk 7m ~ C ~ BUS R1 0:18 0:32 ~ F ~ Walk 7m [0:11 0:39 28m Tₙ0]";
    String expStdOne = "Walk 1s ~ A ~ BUS R1 0:10 0:32 ~ F ~ Walk 7m [0:09:59 0:39 29m1s Tₙ0]";
    String expStdRevOne = "Walk 7m ~ C ~ BUS R1 0:18 0:40 ~ H ~ Walk 1s [0:11 0:40:01 29m1s Tₙ0]";
    return RaptorModuleTestCase.of()
      .withRequest(r -> r.searchParams().timetable(true))
      .addMinDuration("28m", TX_0, T00_00, T01_00)
      .add(standard().manyIterations(), expStd)
      .add(TC_STANDARD_ONE, expStdOne)
      .add(TC_STANDARD_REV_ONE, expStdRevOne);
  }

  static List<RaptorModuleTestCase> testCases() {
    return standardTestCases()
      .add(
        multiCriteria(),
        "Walk 7m ~ C ~ BUS R1 0:18 0:32 ~ F ~ Walk 7m [0:11 0:39 28m Tₙ0 C₁3_120]",
        "Walk 4m ~ B ~ BUS R1 0:14 0:32 ~ F ~ Walk 7m [0:10 0:39 29m Tₙ0 C₁3_000]",
        "Walk 1s ~ A ~ BUS R1 0:10 0:32 ~ F ~ Walk 7m [0:09:59 0:39 29m1s Tₙ0 C₁2_762]",
        "Walk 7m ~ C ~ BUS R1 0:18 0:36 ~ G ~ Walk 4m [0:11 0:40 29m Tₙ0 C₁3_000]",
        "Walk 4m ~ B ~ BUS R1 0:14 0:36 ~ G ~ Walk 4m [0:10 0:40 30m Tₙ0 C₁2_880]",
        "Walk 1s ~ A ~ BUS R1 0:10 0:36 ~ G ~ Walk 4m [0:09:59 0:40 30m1s Tₙ0 C₁2_642]",
        "Walk 7m ~ C ~ BUS R1 0:18 0:40 ~ H ~ Walk 1s [0:11 0:40:01 29m1s Tₙ0 C₁2_762]",
        "Walk 4m ~ B ~ BUS R1 0:14 0:40 ~ H ~ Walk 1s [0:10 0:40:01 30m1s Tₙ0 C₁2_642]",
        "Walk 1s ~ A ~ BUS R1 0:10 0:40 ~ H ~ Walk 1s [0:09:59 0:40:01 30m2s Tₙ0 C₁2_404]"
      )
      .build();
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void testRaptorWithTimeTable(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }

  static List<RaptorModuleTestCase> testCasesWithoutTimetable() {
    return standardTestCases()
      .withRequest(r -> r.searchParams().timetable(false))
      .add(
        multiCriteria(),
        "Walk 7m ~ C ~ BUS R1 0:18 0:32 ~ F ~ Walk 7m [0:11 0:39 28m Tₙ0 C₁3_120]",
        "Walk 4m ~ B ~ BUS R1 0:14 0:32 ~ F ~ Walk 7m [0:10 0:39 29m Tₙ0 C₁3_000]",
        "Walk 1s ~ A ~ BUS R1 0:10 0:32 ~ F ~ Walk 7m [0:09:59 0:39 29m1s Tₙ0 C₁2_762]",
        "Walk 1s ~ A ~ BUS R1 0:10 0:36 ~ G ~ Walk 4m [0:09:59 0:40 30m1s Tₙ0 C₁2_642]",
        "Walk 1s ~ A ~ BUS R1 0:10 0:40 ~ H ~ Walk 1s [0:09:59 0:40:01 30m2s Tₙ0 C₁2_404]"
      )
      .build();
  }

  @ParameterizedTest
  @MethodSource("testCasesWithoutTimetable")
  void testRaptorWithoutTimetable(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }
}
