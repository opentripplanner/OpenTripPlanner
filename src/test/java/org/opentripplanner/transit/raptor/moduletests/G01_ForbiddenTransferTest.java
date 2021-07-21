package org.opentripplanner.transit.raptor.moduletests;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opentripplanner.model.transfer.Transfer.MAX_WAIT_TIME_NOT_SET;
import static org.opentripplanner.model.transfer.TransferPriority.NOT_ALLOWED;
import static org.opentripplanner.transit.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.transit.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.walk;
import static org.opentripplanner.transit.raptor._data.transit.TestTripSchedule.schedule;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.model.transfer.TransferTestData;
import org.opentripplanner.transit.raptor.RaptorService;
import org.opentripplanner.transit.raptor._data.transit.TestTransitData;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.request.RaptorProfile;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.rangeraptor.configure.RaptorConfig;
import org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.transfer.Transfer;

/**
 * FEATURE UNDER TEST
 *
 * Raptor should not return path if it would need to use a NOT_ALLOWED transfers.
 */
public class G01_ForbiddenTransferTest implements RaptorTestConstants {
  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder =
    new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
      RaptorConfig.defaultConfigForTest()
  );

  /**
  * Schedule: Stop:   1       2       3 R1: 00:02 - 00:05 R2:         00:05 - 00:10
  * <p>
  * Access(stop 1) and egress(stop 3) is 30s.
  */
  @Before
  public void setup() {
    Stop OTP_STOP_A = TransferTestData.STOP_A;
    Stop OTP_STOP_B = TransferTestData.STOP_B;
    Stop OTP_STOP_C = TransferTestData.STOP_C;



    var r1 = route("R1", STOP_A, STOP_B)
             .withTimetable(schedule("0:02 0:05"));

    var r2 = route("R2", STOP_B, STOP_C)
             .withTimetable(schedule("0:05 0:10"));

    Transfer t1 = new Transfer(
      TransferTestData.STOP_POINT_B,
      TransferTestData.STOP_POINT_B,
      NOT_ALLOWED,
      false,
      false,
      MAX_WAIT_TIME_NOT_SET
    );

    data.withRoutes(r1, r2);
    data.withStopByIndex(OTP_STOP_A, STOP_A)
        .withStopByIndex(OTP_STOP_B, STOP_B)
        .withStopByIndex(OTP_STOP_C, STOP_C);
    data.withForbiddenTransfer(STOP_B, t1);

    requestBuilder.searchParams()
      .forbiddenTransfersEnabled(true)
      .addAccessPaths(walk(STOP_A, D30s))
      .addEgressPaths(walk(STOP_C, D30s))
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T00_30)
      .timetableEnabled(true);

    requestBuilder.slackProvider(
      RaptorSlackProvider.defaultSlackProvider(0, 0, 0)
    );

      ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  @Test
  public void standard() {
    var request = requestBuilder
        .profile(RaptorProfile.STANDARD)
        .build();

    var response = raptorService.route(request, data);

    assert(response.paths().isEmpty());
  }

  @Test
  public void multiCriteria() {
    var request = requestBuilder
        .profile(RaptorProfile.MULTI_CRITERIA)
        .build();

    var response = raptorService.route(request, data);

    assert(response.paths().isEmpty());
  }
}
