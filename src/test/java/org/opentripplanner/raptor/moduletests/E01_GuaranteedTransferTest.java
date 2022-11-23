package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.Optimization;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.raptor.spi.SearchDirection;

/**
 * FEATURE UNDER TEST
 * <p>
 * Raptor should return a path if it exists when a transfer is only possible because it is
 * guaranteed/stay-seated. A guarantied transfer should be able even if there is zero time to do the
 * transfer. In these cases the transfer-slack should be ignored and the connection should be
 * possible.
 */
public class E01_GuaranteedTransferTest implements RaptorTestConstants {

  private static final String EXP_PATH =
    "Walk 30s ~ A ~ BUS R1 0:02 0:05 ~ B " +
    "~ BUS R2 0:05 0:10 ~ C ~ Walk 30s [0:01:10 0:10:40 9m30s 1tx";
  private static final String EXP_PATH_NO_COST = EXP_PATH + "]";
  private static final String EXP_PATH_WITH_COST = EXP_PATH + " $1230]";
  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  /**
   * Schedule: Stop:   1       2       3 R1: 00:02 - 00:05 R2:         00:05 - 00:10
   * <p>
   * Access(stop 1) and egress(stop 3) is 30s.
   */
  @BeforeEach
  public void setup() {
    var r1 = route("R1", STOP_A, STOP_B).withTimetable(schedule("0:02 0:05"));
    var r2 = route("R2", STOP_B, STOP_C).withTimetable(schedule("0:05 0:10"));

    var tripA = r1.timetable().getTripSchedule(0);
    var tripB = r2.timetable().getTripSchedule(0);

    data.withRoutes(r1, r2);
    data.withGuaranteedTransfer(tripA, STOP_B, tripB, STOP_B);
    data.mcCostParamsBuilder().transferCost(100);

    requestBuilder
      .searchParams()
      .constrainedTransfersEnabled(true)
      .addAccessPaths(TestAccessEgress.walk(STOP_A, D30s))
      .addEgressPaths(TestAccessEgress.walk(STOP_C, D30s))
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T00_30)
      .timetableEnabled(true);

    // Make sure the slack have values which prevent the normal from happening transfer.
    // The test scenario have zero seconds to transfer, so any slack will do.
    requestBuilder.slackProvider(RaptorSlackProvider.defaultSlackProvider(30, 20, 10));

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
  public void standardDynamicSearchWindow() {
    var request = requestBuilder.profile(RaptorProfile.STANDARD).build();
    var response = raptorService.route(request, data);
    assertEquals(EXP_PATH_NO_COST, pathsToString(response));
  }

  @Test
  public void standardReverseOneIteration() {
    var request = requestBuilder
      .searchDirection(SearchDirection.REVERSE)
      .profile(RaptorProfile.STANDARD)
      .searchParams()
      .searchOneIterationOnly()
      .build();

    var response = raptorService.route(request, data);

    assertEquals(EXP_PATH_NO_COST, pathsToString(response));
  }

  @Test
  public void multiCriteria() {
    requestBuilder.optimizations().add(Optimization.PARETO_CHECK_AGAINST_DESTINATION);
    var request = requestBuilder.profile(RaptorProfile.MULTI_CRITERIA).build();

    var response = raptorService.route(request, data);

    assertEquals(EXP_PATH_WITH_COST, pathsToString(response));
  }
}
