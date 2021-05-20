package org.opentripplanner.transit.raptor.moduletests;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.transit.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.transit.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.walk;
import static org.opentripplanner.transit.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.transit.raptor._data.transit.TestTripSchedule.schedule;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.transit.raptor.RaptorService;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.transit.TestTransitData;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.request.RaptorProfile;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.rangeraptor.configure.RaptorConfig;

/**
 * FEATURE UNDER TEST
 * <p>
 * Raptor should return transit option with the lowest cost when to rides are equal, but
 * have different transit-reluctance.
 */
public class F01_TransitModeReluctanceTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
          RaptorConfig.defaultConfigForTest()
  );

  private final static String EXPECTED =  "Walk 30s ~ 1 ~ BUS %s 0:01 0:02:40 ~ 2 ~ Walk 20s "
          + "[0:00:30 0:03 2m30s $%d]";

  @Before
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

    requestBuilder.searchParams()
        .addAccessPaths(walk(STOP_A, D30s))
        .addEgressPaths(walk(STOP_B, D20s))
        .earliestDepartureTime(T00_00)
        .latestArrivalTime(T00_10)
        .timetableEnabled(true);

    requestBuilder.profile(RaptorProfile.MULTI_CRITERIA);

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  @Test
  public void preferR1() {
    // Give R1 a slightly smaller(0.01 less then R2) transit reluctance factor
    requestBuilder.mcCostFactors().transitReluctanceFactors(new double[] {0.99, 1.0 });
    var request = requestBuilder.build();
    var response = raptorService.route(request, data);

    // Verify R1 is preferred and the cost is correct
    assertEquals(expected("R1", 899), pathsToString(response));
  }

  @Test
  public void preferR2() {
    requestBuilder.mcCostFactors().transitReluctanceFactors(new double[] {0.9, 0.89 });
    var request = requestBuilder.build();
    var response = raptorService.route(request, data);

    assertEquals(expected("R2", 889), pathsToString(response));
  }

  private static String expected(String route, int cost) {
    return String.format(EXPECTED, route, cost);
  }
}