package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.free;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.walk;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD_ONE;
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
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCase;

/*
 * FEATURE UNDER TEST
 *
 * Raptor should not route if access is "closed", instead it should find the long access path.
 */
public class G05_ClosedAccessOpeningHoursTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  public void setup() {
    data.withRoute(route("R1", STOP_A, STOP_E).withTimetable(schedule("00:10 00:20")));

    requestBuilder
      .searchParams()
      .earliestDepartureTime(T00_00)
      .searchWindow(Duration.ofMinutes(30))
      .timetable(true)
      .addAccessPaths(walk(STOP_A, D1m).openingHoursClosed(), walk(STOP_A, D7m))
      .addEgressPaths(free(STOP_E));

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  static List<RaptorModuleTestCase> testCases() {
    var expected = "Walk 7m ~ A ~ BUS R1 0:10 0:20 ~ E [0:03 0:20 17m 0tx $2040]";
    return RaptorModuleTestCase
      .of()
      //.add(minDuration(), PathUtils.withoutCost(expected))
      .add(TC_STANDARD, PathUtils.withoutCost(expected))
      .add(TC_STANDARD_ONE, PathUtils.withoutCost(expected))
      //.add(TC_STANDARD_REV, PathUtils.withoutCost(expected))
      //.add(TC_STANDARD_REV_ONE, PathUtils.withoutCost(expected))
      .add(multiCriteria(), expected)
      .build();
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void verifyNo(RaptorModuleTestCase testCase) {
    requestBuilder.searchParams().addAccessPaths(walk(STOP_B, D2m));
    var request = testCase.withConfig(requestBuilder);

    var response = raptorService.route(request, data);

    assertEquals(testCase.expected(), pathsToString(response));
  }
}
