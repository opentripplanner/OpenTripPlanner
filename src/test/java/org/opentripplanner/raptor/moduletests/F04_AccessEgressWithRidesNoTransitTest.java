package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flex;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTransfer.transfer;
import static org.opentripplanner.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MIN_DURATION;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MIN_DURATION_REV;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.multiCriteria;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.standard;

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
import org.opentripplanner.raptor.spi.UnknownPathString;

/**
 * FEATURE UNDER TEST
 * <p>
 * Raptor should be able to time-shift a Flex ~ Walk ~ Flex path, either to the
 * requested departure time (iteration-departure-time) or to the earliest possible
 * time within the opening hours.
 */
public class F04_AccessEgressWithRidesNoTransitTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  public void setup() {
    data
      .withRoute(
        // The route is not reached by Raptor, but we need at least one trip schedule for the test-data to be valid.
        route(pattern("Any", STOP_A, STOP_D))
          .withTimetable(schedule().departures("0:04 0:10").arrivals("0:00 0:06"))
      )
      .withTransfer(STOP_B, transfer(STOP_C, D5m));

    requestBuilder
      .searchParams()
      .addEgressPaths(flex(STOP_C, D2m, ONE_RIDE, 56_000))
      .earliestDepartureTime(T00_10)
      .latestArrivalTime(T00_30)
      .searchWindowInSeconds(D10m);
  }

  static List<RaptorModuleTestCase> flexOnlyNoWalkTestCases() {
    var path = "Flex 2m 1x ~ B ~ Walk 5m ~ C ~ Flex 2m 1x [0:15 0:25 10m 1tx $1620]";
    var stdPathRev = "Flex 2m 1x ~ B ~ Walk 5m ~ C ~ Flex 2m 1x [0:35 0:45 10m 1tx]";
    var minDurationPath = UnknownPathString.of(D10m, 1);
    return RaptorModuleTestCase
      .of()
      .add(TC_MIN_DURATION, minDurationPath.departureAt(T00_10))
      .add(TC_MIN_DURATION_REV, minDurationPath.arrivalAt(T00_30))
      .add(standard().forwardOnly(), PathUtils.withoutCost(path))
      .add(standard().reverseOnly(), stdPathRev)
      .add(multiCriteria(), path)
      .build();
  }

  @ParameterizedTest
  @MethodSource("flexOnlyNoWalkTestCases")
  void flexOnlyNoWalk(RaptorModuleTestCase testCase) {
    requestBuilder.searchParams().addAccessPaths(flex(STOP_B, D2m, ONE_RIDE, 40_000));

    var request = testCase.withConfig(requestBuilder);
    var response = raptorService.route(request, data);
    assertEquals(testCase.expected(), pathsToString(response));
  }

  @ParameterizedTest
  @MethodSource("flexOnlyNoWalkTestCases")
  void flexOnlyNoWalkWithOpening(RaptorModuleTestCase testCase) {
    requestBuilder.searchParams().addAccessPaths(flex(STOP_B, D2m, ONE_RIDE, 40_000));

    var request = testCase.withConfig(requestBuilder);
    var response = raptorService.route(request, data);
    assertEquals(testCase.expected(), pathsToString(response));
  }
}
