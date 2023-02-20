package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MIN_DURATION;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MIN_DURATION_REV;
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
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCase;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCaseBuilder;
import org.opentripplanner.raptor.spi.UnknownPathString;

/**
 * FEATURE UNDER TEST
 * <p>
 * Raptor should return the optimal path with various access and egress paths. The Standard and
 * Standard Reverse optimizations should return the best time option, while the Multi-criteria
 * optimization should return a pareto optimal solutions with/without timetable view enabled.
 */
public class B03_AccessEgressTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  public void setup() {
    data.withRoute(
      route("R1", STOP_A, STOP_B, STOP_C, STOP_D, STOP_E, STOP_F, STOP_G, STOP_H)
        .withTimetable(schedule("0:10, 0:14, 0:18, 0:22, 0:28, 0:32, 0:36, 0:40"))
    );

    requestBuilder
      .searchParams()
      .addAccessPaths(
        TestAccessEgress.walk(STOP_A, D1s), // Lowest cost
        TestAccessEgress.walk(STOP_B, D4m), // Best compromise of cost and time
        TestAccessEgress.walk(STOP_C, D7m), // Latest departure time: 0:16 - 5m = 0:11
        TestAccessEgress.walk(STOP_D, D20m) // Not optimal
      )
      .addEgressPaths(
        TestAccessEgress.walk(STOP_E, D20m), // Not optimal
        TestAccessEgress.walk(STOP_F, D7m), // Earliest arrival time: 0:18 + 3m = 0:21
        TestAccessEgress.walk(STOP_G, D4m), // Best compromise of cost and time
        TestAccessEgress.walk(STOP_H, D1s) // Lowest cost
      )
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T01_00);

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  /** Only the multi-criteria test-cases differ for timetableView on/off */
  private static RaptorModuleTestCaseBuilder standardTestCases() {
    var expMinDuration = UnknownPathString.of("28m", 0);
    String expStd = "Walk 7m ~ C ~ BUS R1 0:18 0:32 ~ F ~ Walk 7m [0:11 0:39 28m 0tx]";
    String expStdOne = "Walk 1s ~ A ~ BUS R1 0:10 0:32 ~ F ~ Walk 7m [0:09:59 0:39 29m1s 0tx]";
    String expStdRevOne = "Walk 7m ~ C ~ BUS R1 0:18 0:40 ~ H ~ Walk 1s [0:11 0:40:01 29m1s 0tx]";
    return RaptorModuleTestCase
      .of()
      .add(TC_MIN_DURATION, expMinDuration.departureAt("0:00"))
      .add(TC_MIN_DURATION_REV, expMinDuration.arrivalAt("01:00"))
      .add(standard().manyIterations(), expStd)
      .add(TC_STANDARD_ONE, expStdOne)
      .add(TC_STANDARD_REV_ONE, expStdRevOne);
  }

  static List<RaptorModuleTestCase> testCases() {
    return standardTestCases()
      .add(
        multiCriteria(),
        "Walk 7m ~ C ~ BUS R1 0:18 0:32 ~ F ~ Walk 7m [0:11 0:39 28m 0tx $3120]",
        "Walk 4m ~ B ~ BUS R1 0:14 0:32 ~ F ~ Walk 7m [0:10 0:39 29m 0tx $3000]",
        "Walk 1s ~ A ~ BUS R1 0:10 0:32 ~ F ~ Walk 7m [0:09:59 0:39 29m1s 0tx $2762]",
        "Walk 7m ~ C ~ BUS R1 0:18 0:36 ~ G ~ Walk 4m [0:11 0:40 29m 0tx $3000]",
        "Walk 4m ~ B ~ BUS R1 0:14 0:36 ~ G ~ Walk 4m [0:10 0:40 30m 0tx $2880]",
        "Walk 1s ~ A ~ BUS R1 0:10 0:36 ~ G ~ Walk 4m [0:09:59 0:40 30m1s 0tx $2642]",
        "Walk 7m ~ C ~ BUS R1 0:18 0:40 ~ H ~ Walk 1s [0:11 0:40:01 29m1s 0tx $2762]",
        "Walk 4m ~ B ~ BUS R1 0:14 0:40 ~ H ~ Walk 1s [0:10 0:40:01 30m1s 0tx $2642]",
        "Walk 1s ~ A ~ BUS R1 0:10 0:40 ~ H ~ Walk 1s [0:09:59 0:40:01 30m2s 0tx $2404]"
      )
      .build();
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void testRaptorWithTimeTable(RaptorModuleTestCase testCase) {
    requestBuilder.searchParams().timetable(true);
    var request = testCase.withConfig(requestBuilder);
    var response = raptorService.route(request, data);
    assertEquals(testCase.expected(), pathsToString(response));
  }

  static List<RaptorModuleTestCase> testCasesWithoutTimetable() {
    return standardTestCases()
      .add(
        multiCriteria(),
        "Walk 7m ~ C ~ BUS R1 0:18 0:32 ~ F ~ Walk 7m [0:11 0:39 28m 0tx $3120]",
        "Walk 4m ~ B ~ BUS R1 0:14 0:32 ~ F ~ Walk 7m [0:10 0:39 29m 0tx $3000]",
        "Walk 1s ~ A ~ BUS R1 0:10 0:32 ~ F ~ Walk 7m [0:09:59 0:39 29m1s 0tx $2762]",
        "Walk 1s ~ A ~ BUS R1 0:10 0:36 ~ G ~ Walk 4m [0:09:59 0:40 30m1s 0tx $2642]",
        "Walk 1s ~ A ~ BUS R1 0:10 0:40 ~ H ~ Walk 1s [0:09:59 0:40:01 30m2s 0tx $2404]"
      )
      .build();
  }

  @ParameterizedTest
  @MethodSource("testCasesWithoutTimetable")
  void testRaptorWithoutTimetable(RaptorModuleTestCase testCase) {
    requestBuilder.searchParams().timetable(false);
    var request = testCase.withConfig(requestBuilder);
    var response = raptorService.route(request, data);
    assertEquals(testCase.expected(), pathsToString(response));
  }
}
