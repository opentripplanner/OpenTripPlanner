package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToStringDetailed;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MIN_DURATION;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MIN_DURATION_REV;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD_ONE;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.multiCriteria;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.standard;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCase;
import org.opentripplanner.raptor.spi.UnknownPathString;

/**
 * FEATURE UNDER TEST
 * <p>
 * Raptor should return the optimal path with various access paths. All Raptor
 * optimizations(McRaptor, Standard and Reverse Standard) should be tested.
 */
public class B01_AccessTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  void setup() {
    data.withRoute(
      route("R1", STOP_B, STOP_C, STOP_D, STOP_E, STOP_F)
        .withTimetable(schedule("0:10 0:14 0:18 0:22 0:25"))
    );

    requestBuilder
      .searchParams()
      .addAccessPaths(
        TestAccessEgress.walk(STOP_B, D1s), // Lowest cost
        TestAccessEgress.walk(STOP_C, D4m), // Best compromise of cost and time
        TestAccessEgress.walk(STOP_D, DurationUtils.durationInSeconds("7m")), // Latest departure time
        TestAccessEgress.walk(STOP_E, DurationUtils.durationInSeconds("13m")) // Not optimal
      )
      .addEgressPaths(TestAccessEgress.walk(STOP_F, D1s))
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T00_30)
      .timetable(true)
      // Removing the search-window should not have any effect, but it does.
      .searchWindow(Duration.ofMinutes(20));

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  static List<RaptorModuleTestCase> testCases() {
    var expMinDuration = UnknownPathString.of("14m1s", 0);
    var expStd =
      "Walk 7m 0:11 0:18 ~ D 0s ~ BUS R1 0:18 0:25 7m ~ F 0s ~ Walk 1s 0:25 0:25:01 [0:11 0:25:01 14m1s 0tx]";
    var expStdOne =
      "Walk 1s 0:09:59 0:10 ~ B 0s ~ BUS R1 0:10 0:25 15m ~ F 0s ~ Walk 1s 0:25 0:25:01 [0:09:59 0:25:01 15m2s 0tx]";

    return RaptorModuleTestCase
      .of()
      .add(TC_MIN_DURATION, expMinDuration.departureAt(T00_00))
      .add(TC_MIN_DURATION_REV, expMinDuration.arrivalAt(T00_30))
      .add(standard().not(TC_STANDARD_ONE), expStd)
      // When we run one iteration the  first access boarding is used as long as it
      // arrives at the origin at the same time. In this case the "worst" path is kept.
      .add(TC_STANDARD_ONE, expStdOne)
      .add(
        multiCriteria(),
        "Walk 7m 0:11 0:18 $840 ~ D 0s ~ BUS R1 0:18 0:25 7m $1020 ~ F 0s ~ Walk 1s 0:25 0:25:01 $2 [0:11 0:25:01 14m1s 0tx $1862]",
        "Walk 4m 0:10 0:14 $480 ~ C 0s ~ BUS R1 0:14 0:25 11m $1260 ~ F 0s ~ Walk 1s 0:25 0:25:01 $2 [0:10 0:25:01 15m1s 0tx $1742]",
        "Walk 1s 0:09:59 0:10 $2 ~ B 0s ~ BUS R1 0:10 0:25 15m $1500 ~ F 0s ~ Walk 1s 0:25 0:25:01 $2 [0:09:59 0:25:01 15m2s 0tx $1504]"
      )
      .build();
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void testRaptor(RaptorModuleTestCase testCase) {
    var request = testCase.withConfig(requestBuilder);
    var response = raptorService.route(request, data);
    assertEquals(testCase.expected(), pathsToStringDetailed(response));
  }
}
