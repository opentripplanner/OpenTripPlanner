package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.withoutCost;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.walk;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.multiCriteria;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
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
public class G04_EgressWithOpeningHoursMultipleOptionsTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder =
    new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  public void setup() {
    data.withRoute(
      route("R1", STOP_B, STOP_C).withTimetable(schedule("00:05 00:15"), schedule("00:10 00:20"))
    );
    requestBuilder
      .searchParams()
      .addAccessPaths(walk(STOP_B, D2m))
      .addEgressPaths(
        walk(STOP_C, D1m).openingHours("0:17", "0:19"),
        walk(STOP_C, D1m).openingHours("0:22", "0:24")
      );

    requestBuilder
      .searchParams()
      .earliestDepartureTime(T00_00)
      .searchWindow(Duration.ofMinutes(30))
      .timetable(true);

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  static List<RaptorModuleTestCase> openInWholeSearchIntervalTestCases() {
    var expA =
      "Walk 2m 0:03 0:05 C₁240 ~ B 0s " +
      "~ BUS R1 0:05 0:15 10m C₁1_200 ~ C 2m " +
      "~ Walk 1m Open(0:17 0:19) 0:17 0:18 C₁240 " +
      "[0:03 0:18 15m Tₓ0 C₁1_680]";

    var expB =
      "Walk 2m 0:08 0:10 C₁240 ~ B 0s " +
      "~ BUS R1 0:10 0:20 10m C₁1_200 ~ C 2m " +
      "~ Walk 1m Open(0:22 0:24) 0:22 0:23 C₁240 " +
      "[0:08 0:23 15m Tₓ0 C₁1_680]";

    return RaptorModuleTestCase.of()
      .add(TC_STANDARD, withoutCost(expA, expB))
      //.add(TC_STANDARD_ONE, withoutCost(expA))
      // TODO - Find out why the reverse Standard profiles does not return anything
      //      - .add(standard().reverseOnly(), withoutCost("???"))
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
