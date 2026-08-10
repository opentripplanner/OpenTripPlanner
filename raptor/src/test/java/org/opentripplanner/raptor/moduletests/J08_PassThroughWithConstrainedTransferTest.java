package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor._data.transit.TestTransfer.transfer;
import static org.opentripplanner.raptor.api.request.via.RaptorViaLocation.passThrough;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestTransferConstraint;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.configure.RaptorTestFactory;
import org.opentripplanner.raptor.spi.TestSlackProvider;

/**
 * FEATURE UNDER TEST
 * <p>
 * When a trip passes through a pass-through via point and the boarding was via a constrained
 * transfer (stay-seated or guaranteed), {@code continueOnSameTripInNextSegment} must correctly
 * reconstruct the boarding stop position for the trip in the next Raptor segment.
 * <p>
 * For a <b>guaranteed (walk) transfer</b>, the walk-arrival time at the boarding stop may exceed
 * the trip's scheduled departure (the train is held), so a time-based stop-position search fails.
 * The fallback must find the stop position without relying on the walk-arrival time.
 */
class J08_PassThroughWithConstrainedTransferTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorService<TestTripSchedule> raptorService = RaptorTestFactory.raptorService();

  /**
   * Stay-seated pass-through: a feeder trip (R1) connects via a stay-seated transfer to a main
   * trip (R2). The main trip passes through the via point C before reaching the destination D.
   */
  @Test
  void staySeatedTransferWithPassThrough() {
    data
      .access("Walk 30s ~ A")
      .withTimetables(
        """
        -- R1
        A     B
        0:02  0:05
        -- R2
              B     C     D
              0:05  0:08  0:12
        """
      )
      .egress("D ~ Walk 30s");

    var tripR1 = data.getRoute(0).getTripSchedule(0);
    var tripR2 = data.getRoute(1).getTripSchedule(0);

    data.withConstrainedTransfer(
      tripR1,
      STOP_B,
      tripR2,
      STOP_B,
      TestTransferConstraint.staySeated()
    );
    data.withSlackProvider(new TestSlackProvider(D30_s, D20_s, D10_s));

    var requestBuilder = data.requestBuilder();
    requestBuilder.profile(RaptorProfile.MULTI_CRITERIA).clearOptimizations();
    requestBuilder
      .searchParams()
      .constrainedTransfers(true)
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T00_30)
      .searchWindow(Duration.ofMinutes(15))
      .timetable(true)
      .addViaLocation(passThrough("C").addStop(STOP_C).build());

    assertEquals(
      "Walk 30s ~ A ~ BUS R1 0:02 0:05 ~ B ~ BUS R2 0:05 0:12 ~ D ~ Walk 30s [0:01:10 0:12:40 11m30s Tₙ0 C₁1_350]",
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }

  /**
   * Guaranteed walk transfer with pass-through: a feeder bus (R1) connects via a guaranteed
   * interchange with a 30s walk to the main train (R2). The walk arrival at the boarding stop
   * exceeds R2's scheduled departure — the constrained transfer holds the train. R2 then passes
   * through the via point D before reaching the destination E.
   * <p>
   * Without the fix, {@code findDepartureStopPosition(walkArrivalTime=330s, C)} fails because
   * R2 departed C at 300s — causing the pass-through continuation to be silently dropped.
   */
  @Test
  void guaranteedWalkTransferWithPassThrough() {
    data
      .access("Walk 30s ~ A")
      .withTimetables(
        """
        -- R1
        A     B
        0:02  0:05
        -- R2
        C     D     E
        0:05  0:08  0:12
        """
      )
      .egress("E ~ Walk 30s");

    var tripR1 = data.getRoute(0).getTripSchedule(0);
    var tripR2 = data.getRoute(1).getTripSchedule(0);

    data.withGuaranteedTransfer(tripR1, STOP_B, tripR2, STOP_C);
    data.withTransfer(STOP_B, transfer(STOP_C, D30_s));
    data.withSlackProvider(new TestSlackProvider(D30_s, D20_s, D10_s));

    var requestBuilder = data.requestBuilder();
    requestBuilder.profile(RaptorProfile.MULTI_CRITERIA).clearOptimizations();
    requestBuilder
      .searchParams()
      .constrainedTransfers(true)
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T00_30)
      .searchWindow(Duration.ofMinutes(15))
      .timetable(true)
      .addViaLocation(passThrough("D").addStop(STOP_D).build());

    assertEquals(
      "Walk 30s ~ A ~ BUS R1 0:02 0:05 ~ B ~ Walk 30s ~ C ~ BUS R2 0:05 0:12 ~ E ~ Walk 30s [0:01:10 0:12:40 11m30s Tₙ1 C₁1_380]",
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }
}
