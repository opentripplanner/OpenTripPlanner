package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MIN_DURATION;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MIN_DURATION_REV;
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
import org.opentripplanner.raptor._data.api.PathUtils;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCase;

/**
 * FEATURE UNDER TEST
 * <p>
 * Raptor should return a path starting before the search-window, if a search-window-access-slack
 * is used.
 */
public class J01_SearchWindowAccessSlack implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  /**
   * Schedule:
   *   Stop:    A       B
   *    R1:   00:10 - 00:20
   *
   * Access (toStop & duration):
   *   A  1m
   *
   * Egress (fromStop & duration):
   *   B  30s
   */
  @BeforeEach
  void setup() {
    data.withRoute(route(pattern("R1", STOP_A, STOP_B)).withTimetable(schedule("00:10 00:20")));
    requestBuilder
      .searchParams()
      .addAccessPaths(TestAccessEgress.walk(STOP_A, D1m))
      .addEgressPaths(TestAccessEgress.walk(STOP_B, D30s))
      .latestArrivalTime(T00_30)
      .timetable(true);

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  static List<RaptorModuleTestCase> testCases() {
    var path = "Walk 1m ~ A ~ BUS R1 0:10 0:20 ~ B ~ Walk 30s [0:09 0:20:30 11m30s 0tx $1380]";
    return RaptorModuleTestCase
      .of()
      .add(TC_MIN_DURATION, "[0:09 0:20:30 11m30s 0tx]")
      .add(TC_MIN_DURATION_REV, "[0:18:30 0:30 11m30s 0tx]")
      .add(standard(), PathUtils.withoutCost(path))
      .add(multiCriteria(), path)
      .build();
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void testRaptor1mSlack(RaptorModuleTestCase testCase) {
    requestBuilder
      .searchParams()
      .earliestDepartureTime(T00_12)
      .searchWindowAccessSlack(Duration.ofMinutes(3));

    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void testRaptorNotEnoughSlack(RaptorModuleTestCase testCase) {
    requestBuilder
      .searchParams()
      .earliestDepartureTime(T00_12)
      .searchWindowAccessSlack(DurationUtils.duration("2m59s"));

    if (testCase.config() != TC_MIN_DURATION_REV) {
      assertEquals("", testCase.run(raptorService, data, requestBuilder));
    } else {
      assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
    }
  }
}
