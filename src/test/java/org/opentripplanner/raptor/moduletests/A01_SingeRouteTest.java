package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.spi.SearchDirection;

/**
 * FEATURE UNDER TEST
 * <p>
 * Raptor should return a path if it exist for the most basic case with one route with one trip, an
 * access and an egress path.
 */
public class A01_SingeRouteTest implements RaptorTestConstants {

  private static final String EXP_PATH =
    "Walk 30s ~ B ~ BUS R1 0:01 0:05 ~ D ~ Walk 20s " + "[0:00:30 0:05:20 4m50s 0tx";
  private static final String EXP_PATH_NO_COST = EXP_PATH + "]";
  private static final String EXP_PATH_WITH_COST = EXP_PATH + " $940]";

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  /**
   * Stops: 0..3
   *
   * Stop on route (stop indexes):
   *   R1:  1 - 2 - 3
   *
   * Schedule:
   *   R1: 00:01 - 00:03 - 00:05
   *
   * Access (toStop & duration):
   *   1  30s
   *
   * Egress (fromStop & duration):
   *   3  20s
   */
  @BeforeEach
  public void setup() {
    data.withRoute(
      route(pattern("R1", STOP_B, STOP_C, STOP_D)).withTimetable(schedule("00:01, 00:03, 00:05"))
    );
    requestBuilder
      .searchParams()
      .addAccessPaths(TestAccessEgress.walk(STOP_B, D30s))
      .addEgressPaths(TestAccessEgress.walk(STOP_D, D20s))
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T00_10)
      .timetableEnabled(true);

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  @Test
  public void standardOneIteration() {
    var request = requestBuilder
      .profile(RaptorProfile.STANDARD)
      .searchParams()
      .searchOneIterationOnly()
      .build();

    var response = raptorService.route(request, data);

    assertEquals(EXP_PATH_NO_COST, pathsToString(response));
  }

  @Test
  public void standardReverseWithoutSearchWindow() {
    var request = requestBuilder
      .searchDirection(SearchDirection.REVERSE)
      .profile(RaptorProfile.STANDARD)
      .build();

    var response = raptorService.route(request, data);

    assertEquals(EXP_PATH_NO_COST, pathsToString(response));
  }

  @Test
  public void multiCriteria() {
    var request = requestBuilder.profile(RaptorProfile.MULTI_CRITERIA).build();

    var response = raptorService.route(request, data);

    assertEquals(EXP_PATH_WITH_COST, pathsToString(response));
  }
}
