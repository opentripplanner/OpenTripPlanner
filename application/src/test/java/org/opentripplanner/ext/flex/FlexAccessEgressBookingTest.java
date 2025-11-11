package org.opentripplanner.ext.flex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_00;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.flex.trip.UnscheduledTrip;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.TestStateBuilder;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;

class FlexAccessEgressBookingTest {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();

  private static StopTime stopWithWindowAndPickupBooking(int start, int end, BookingInfo booking) {
    var st = new StopTime();
    st.setStop(TEST_MODEL.stop("S").build());
    st.setFlexWindowStart(start);
    st.setFlexWindowEnd(end);
    st.setPickupBookingInfo(booking);
    return st;
  }

  private static StopTime stopWithWindow(int start, int end) {
    var st = new StopTime();
    st.setStop(TEST_MODEL.stop("S").build());
    st.setFlexWindowStart(start);
    st.setFlexWindowEnd(end);
    return st;
  }

  private static FlexAccessEgress buildFlexAccessEgress(
    List<StopTime> stopTimes,
    int boardPos,
    int alightPos,
    int requestedBookingTime
  ) {
    var trip = UnscheduledTrip.of(TimetableRepositoryForTest.id("flex"))
      .withTrip(TimetableRepositoryForTest.trip("t1").build())
      .withStopTimes(stopTimes)
      .build();

    State state = TestStateBuilder.ofWalking().streetEdge().build();
    RegularStop stop = (RegularStop) stopTimes.get(boardPos).getStop();
    var durations = new FlexPathDurations(0, 0, 0, 0);
    return new FlexAccessEgress(
      stop,
      durations,
      boardPos,
      alightPos,
      trip,
      state,
      true,
      requestedBookingTime
    );
  }

  @Test
  void earliestDepartureAppliesMinimumBookingNotice() {
    int T10_00 = LocalTime.of(10, 0).toSecondOfDay();
    int T12_00 = LocalTime.of(12, 0).toSecondOfDay();

    var noticeDuration = Duration.ofMinutes(30);
    int noticeSec = (int) noticeDuration.toSeconds();

    var booking = BookingInfo.of().withMinimumBookingNotice(noticeDuration).build();

    var st0 = stopWithWindowAndPickupBooking(T10_00, T12_00, booking);
    var st1 = stopWithWindow(T10_00, T12_00);

    // requested booking time 10:00, ask to depart at 10:05 -> should be shifted to 10:30
    var flex = buildFlexAccessEgress(List.of(st0, st1), 0, 1, T10_00);

    int askedDeparture = T10_00 + 5 * 60;
    int expectedEarliest = T10_00 + noticeSec; // 10:30
    assertEquals(expectedEarliest, flex.earliestDepartureTime(askedDeparture));
  }

  @Test
  void latestArrivalRespectsMinimumBookingNotice() {
    int T10_00 = LocalTime.of(10, 0).toSecondOfDay();

    var noticeDuration = Duration.ofMinutes(45);
    var booking = BookingInfo.of().withMinimumBookingNotice(noticeDuration).build();

    var st0 = stopWithWindowAndPickupBooking(T10_00, T11_00, booking);
    var st1 = stopWithWindow(T10_00, T11_00);

    // booking made at 10:00
    var flex = buildFlexAccessEgress(List.of(st0, st1), 0, 1, T10_00);

    // try to arrive at 10:30 (within notice -> should be rejected)
    int arrivalTooEarly = T10_00 + 30 * 60;
    assertEquals(
      org.opentripplanner.model.StopTime.MISSING_VALUE,
      flex.latestArrivalTime(arrivalTooEarly)
    );

    // 10:50 should be allowed (>= 10:45)
    int arrivalOk = T10_00 + 50 * 60;
    assertEquals(arrivalOk, flex.latestArrivalTime(arrivalOk));
  }

  @Test
  void latestArrivalWithinWindowIsReturned() {
    int T10_00 = LocalTime.of(10, 0).toSecondOfDay();
    int T12_00 = LocalTime.of(12, 0).toSecondOfDay();

    var st0 = stopWithWindow(T10_00, T12_00);
    var st1 = stopWithWindow(T10_00, T12_00);

    var flex = buildFlexAccessEgress(
      List.of(st0, st1),
      0,
      1,
      org.opentripplanner.transit.model.timetable.booking.RoutingBookingInfo.NOT_SET
    );

    int requestedArrival = T11_00;
    assertEquals(requestedArrival, flex.latestArrivalTime(requestedArrival));
  }
}
