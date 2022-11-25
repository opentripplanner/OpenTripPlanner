package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flex;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.spi.SearchDirection;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.RaptorCostConverter;

/**
 * FEATURE UNDER TEST
 * <p>
 * Raptor should support combining multiple features, like Flexible access paths and constrained
 * transfers. This test has only one path available, and it is expected that it should be returned
 * irrespective of the profile.
 */
public class E11_GuaranteedTransferWithFlexAccessTest implements RaptorTestConstants {

  private static final int COST_ONE_STOP = RaptorCostConverter.toRaptorCost(2 * 60);

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  public void setup() {
    var r1 = route("R1", STOP_B, STOP_C).withTimetable(schedule("0:30 0:45"));
    var r2 = route("R2", STOP_C, STOP_D).withTimetable(schedule("0:45 0:55"));

    var tripA = r1.timetable().getTripSchedule(0);
    var tripB = r2.timetable().getTripSchedule(0);

    data.withRoutes(r1, r2);
    data.withGuaranteedTransfer(tripA, STOP_C, tripB, STOP_C);
    data.withTransfer(STOP_A, TestTransfer.transfer(STOP_B, D10m));
    data.mcCostParamsBuilder().transferCost(100);

    requestBuilder
      .searchParams()
      .addAccessPaths(flex(STOP_A, D3m, ONE_RIDE, 2 * COST_ONE_STOP))
      .addEgressPaths(TestAccessEgress.walk(STOP_D, D1m));

    requestBuilder
      .searchParams()
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T01_00)
      .constrainedTransfersEnabled(true);

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  @Test
  public void standard() {
    requestBuilder.profile(RaptorProfile.STANDARD);
    requestBuilder.searchParams().searchOneIterationOnly();

    var response = raptorService.route(requestBuilder.build(), data);

    assertEquals(
      "Flex 3m 1x ~ A ~ Walk 10m ~ B ~ BUS R1 0:30 0:45 ~ C ~ BUS R2 0:45 0:55 ~ D ~ Walk 1m [0:16 0:56 40m 2tx]",
      pathsToString(response)
    );
  }

  @Test
  public void standardReverse() {
    requestBuilder.profile(RaptorProfile.STANDARD).searchDirection(SearchDirection.REVERSE);
    requestBuilder.searchParams().searchOneIterationOnly();

    var response = raptorService.route(requestBuilder.build(), data);

    assertEquals(
      "Flex 3m 1x ~ A ~ Walk 10m ~ B ~ BUS R1 0:30 0:45 ~ C ~ BUS R2 0:45 0:55 ~ D ~ Walk 1m [0:16 0:56 40m 2tx]",
      pathsToString(response)
    );
  }

  @Test
  public void multiCriteria() {
    requestBuilder.profile(RaptorProfile.MULTI_CRITERIA);

    var response = raptorService.route(requestBuilder.build(), data);

    assertEquals(
      "Flex 3m 1x ~ A ~ Walk 10m ~ B ~ BUS R1 0:30 0:45 ~ C ~ BUS R2 0:45 0:55 ~ D ~ Walk 1m [0:16 0:56 40m 2tx $3820]",
      pathsToString(response)
    );
  }
}
