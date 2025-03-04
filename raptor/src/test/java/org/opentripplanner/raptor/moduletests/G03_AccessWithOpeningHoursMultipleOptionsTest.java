package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.walk;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD_ONE;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD_REV;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD_REV_ONE;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.multiCriteria;

import java.time.Duration;
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

/*
 * FEATURE UNDER TEST
 *
 * Raptor should take into account opening hours/time restrictions on access. If the time
 * restrictions require it, there should be a wait before boarding the trip so that the access is
 * traversed while "open".
 */
public class G03_AccessWithOpeningHoursMultipleOptionsTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder =
    new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  public void setup() {
    data.withRoute(
      route("R1", STOP_B, STOP_C).withTimetable(schedule("00:10 00:20"), schedule("00:15 00:25"))
    );
    requestBuilder
      .searchParams()
      .addAccessPaths(
        walk(STOP_B, D1m).openingHours("0:05", "0:08"),
        walk(STOP_B, D1m).openingHours("0:10", "0:13")
      )
      .addEgressPaths(walk(STOP_C, D2m));

    requestBuilder
      .searchParams()
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T00_30)
      .searchWindow(Duration.ofMinutes(30))
      .timetable(true);

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  static List<RaptorModuleTestCase> openInWholeSearchIntervalTestCases() {
    var expA =
      "Walk 1m Open(0:05 0:08) 0:08 0:09 C₁120 ~ B 1m " +
      "~ BUS R1 0:10 0:20 10m C₁1_260 ~ C 0s " +
      "~ Walk 2m 0:20 0:22 C₁240 " +
      "[0:08 0:22 14m Tₓ0 C₁1_620]";

    var expB =
      "Walk 1m Open(0:10 0:13) 0:13 0:14 C₁120 ~ B 1m " +
      "~ BUS R1 0:15 0:25 10m C₁1_260 ~ C 0s " +
      "~ Walk 2m 0:25 0:27 C₁240 " +
      "[0:13 0:27 14m Tₓ0 C₁1_620]";

    return RaptorModuleTestCase.of()
      .addMinDuration("13m", TX_0, T00_00, T00_30)
      .add(TC_STANDARD, PathUtils.withoutCost(expA, expB))
      .add(TC_STANDARD_ONE, PathUtils.withoutCost(expA))
      .add(TC_STANDARD_REV, PathUtils.withoutCost(expA, expB))
      .add(TC_STANDARD_REV_ONE, PathUtils.withoutCost(expB))
      .add(multiCriteria(), expA, expB)
      .build();
  }

  @ParameterizedTest
  @MethodSource("openInWholeSearchIntervalTestCases")
  void openInWholeSearchIntervalTest(RaptorModuleTestCase testCase) {
    assertEquals(
      testCase.expected(),
      testCase.runDetailedResult(raptorService, data, requestBuilder)
    );
  }
}
