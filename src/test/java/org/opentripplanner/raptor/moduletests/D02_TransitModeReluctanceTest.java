package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.moduletests.support.ModuleTestDebugLogging;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig;

/**
 * FEATURE UNDER TEST
 * <p>
 * Raptor should return transit option with the lowest cost when to rides are equal, but have
 * different transit-reluctance.
 */
public class D02_TransitModeReluctanceTest implements RaptorTestConstants {

  public static final double[] PREFER_R1 = { 0.99, 1.0 };
  public static final double[] PREFER_R2 = { 0.9, 0.89 };
  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  public void setup() {
    // Given 2 identical routes R1 and R2
    data.withRoute(
      route(pattern("R1", STOP_A, STOP_B))
        .withTimetable(schedule("00:01, 00:02:40").transitReluctanceIndex(0))
    );
    data.withRoute(
      route(pattern("R2", STOP_A, STOP_B))
        .withTimetable(schedule("00:01, 00:02:40").transitReluctanceIndex(1))
    );

    requestBuilder
      .searchParams()
      .addAccessPaths(TestAccessEgress.walk(STOP_A, D30s))
      .addEgressPaths(TestAccessEgress.walk(STOP_B, D20s))
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T00_10)
      .timetable(true);

    requestBuilder.profile(RaptorProfile.MULTI_CRITERIA);

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  static Stream<Arguments> testCases() {
    return RaptorModuleTestConfig
      .multiCriteria()
      .build()
      .stream()
      .flatMap(config ->
        Stream.of(
          Arguments.of(
            PREFER_R1,
            config,
            "Walk 30s ~ A ~ BUS R1 0:01 0:02:40 ~ B ~ Walk 20s " + "[0:00:30 0:03 2m30s Tₓ0 C₁799]"
          ),
          Arguments.of(
            PREFER_R2,
            config,
            "Walk 30s ~ A ~ BUS R2 0:01 0:02:40 ~ B ~ Walk 20s " + "[0:00:30 0:03 2m30s Tₓ0 C₁789]"
          )
        )
      );
  }

  @ParameterizedTest(name = "Transit reluctance [R1, R2]: {0}, profile: {1}")
  @MethodSource("testCases")
  void testTransitReluctance(
    double[] transitReluctance,
    RaptorModuleTestConfig testConfig,
    String expected
  ) {
    data.mcCostParamsBuilder().transitReluctanceFactors(transitReluctance);
    var request = testConfig.apply(requestBuilder).build();
    var response = raptorService.route(request, data);
    assertEquals(expected, pathsToString(response));
  }
}
