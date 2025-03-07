package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.free;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.walk;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.multiCriteria;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.standard;

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
 * Raptor should not route if egress is "closed", instead it should find the long egress path.
 */
public class G06_ClosedEgressOpeningHoursTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder =
    new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  public void setup() {
    data.withRoute(route("R1", STOP_A, STOP_E).withTimetable(schedule("00:05 00:10")));

    requestBuilder
      .searchParams()
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T00_30)
      .searchWindow(Duration.ofMinutes(20))
      .timetable(true)
      .addAccessPaths(free(STOP_A))
      .addEgressPaths(walk(STOP_E, D1m).openingHoursClosed(), walk(STOP_E, D5m));

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  static List<RaptorModuleTestCase> testCases() {
    var expected = "A ~ BUS R1 0:05 0:10 ~ E ~ Walk 5m [0:05 0:15 10m Tₓ0 C₁1_500]";

    return RaptorModuleTestCase.of()
      .withRequest(r -> r.searchParams().addAccessPaths(walk(STOP_B, D2m)))
      .addMinDuration("10m", TX_0, T00_00, T00_30)
      .add(standard(), PathUtils.withoutCost(expected))
      .add(multiCriteria(), expected)
      .build();
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void verifyNo(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }
}
