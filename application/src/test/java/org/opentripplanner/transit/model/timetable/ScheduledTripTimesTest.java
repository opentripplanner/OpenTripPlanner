package org.opentripplanner.transit.model.timetable;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.util.BitSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.framework.DataValidationException;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.time.TimeUtils;

class ScheduledTripTimesTest {

  private static final Trip TRIP = TimetableRepositoryForTest.trip("Trip-1").build();

  private static final List<FeedScopedId> STOP_IDS = List.of(id("A"), id("B"), id("C"));
  private static final int SERVICE_CODE = 5;
  private static final BitSet TIMEPOINTS = new BitSet(3);
  private static final int T10_00 = TimeUtils.time("10:00");
  private static final int T10_01 = T10_00 + 60;
  private static final int T11_00 = TimeUtils.time("11:00");
  private static final int T11_02 = T11_00 + 120;
  private static final int T12_00 = TimeUtils.time("12:00");
  private static final int T12_03 = T12_00 + 180;
  public static final int STOP_POS_0 = 0;
  public static final int STOP_POS_1 = 1;
  public static final int STOP_POS_2 = 2;

  static {
    TIMEPOINTS.set(1);
  }

  private final ScheduledTripTimes subject = ScheduledTripTimes.of()
    .withArrivalTimes("10:00 11:00 12:00")
    .withDepartureTimes("10:01 11:02 12:03")
    .withServiceCode(SERVICE_CODE)
    .withTrip(TRIP)
    .withTimepoints(TIMEPOINTS)
    .build();

  @Test
  void getServiceCode() {
    assertEquals(SERVICE_CODE, subject.getServiceCode());
  }

  @Test
  void getScheduledArrivalTime() {
    assertEquals(T10_00, subject.getScheduledArrivalTime(STOP_POS_0));
    assertEquals(T11_00, subject.getScheduledArrivalTime(STOP_POS_1));
    assertEquals(T12_00, subject.getScheduledArrivalTime(STOP_POS_2));
  }

  @Test
  void getArrivalTime() {
    assertEquals(T10_00, subject.getArrivalTime(STOP_POS_0));
    assertEquals(T11_00, subject.getArrivalTime(STOP_POS_1));
    assertEquals(T12_00, subject.getArrivalTime(STOP_POS_2));
  }

  @Test
  void getArrivalDelay() {
    assertEquals(0, subject.getArrivalDelay(STOP_POS_0));
    assertEquals(0, subject.getArrivalDelay(STOP_POS_1));
    assertEquals(0, subject.getArrivalDelay(STOP_POS_2));
  }

  @Test
  void getScheduledDepartureTime() {
    assertEquals(T10_01, subject.getScheduledDepartureTime(STOP_POS_0));
    assertEquals(T11_02, subject.getScheduledDepartureTime(STOP_POS_1));
    assertEquals(T12_03, subject.getScheduledDepartureTime(STOP_POS_2));
  }

  @Test
  void getDepartureTime() {
    assertEquals(T10_01, subject.getDepartureTime(STOP_POS_0));
    assertEquals(T11_02, subject.getDepartureTime(STOP_POS_1));
    assertEquals(T12_03, subject.getDepartureTime(STOP_POS_2));
  }

  @Test
  void getDepartureDelay() {
    assertEquals(0, subject.getDepartureDelay(STOP_POS_0));
    assertEquals(0, subject.getDepartureDelay(STOP_POS_1));
    assertEquals(0, subject.getDepartureDelay(STOP_POS_2));
  }

  @Test
  void isTimepoint() {
    assertFalse(subject.isTimepoint(STOP_POS_0));
    assertTrue(subject.isTimepoint(STOP_POS_1));
    assertFalse(subject.isTimepoint(STOP_POS_2));
  }

  @Test
  void validateLastArrivalTimeIsNotMoreThan20DaysAfterFirstDepartureTime() {
    var ex = assertThrows(DataValidationException.class, () ->
      ScheduledTripTimes.of()
        .withDepartureTimes("10:00 12:00 10:00:01+20d")
        .withServiceCode(SERVICE_CODE)
        .withTrip(TRIP)
        .build()
    );
    assertEquals(
      "The arrivalTime is not in range[-12h, 20d]. Time: 10:00:01+20d, stop-pos: 2, trip: F:Trip-1.",
      ex.getMessage()
    );
  }

  @Test
  void getTrip() {
    assertEquals(TRIP, subject.getTrip());
  }

  @Test
  void sortIndex() {
    assertEquals(T10_01, subject.sortIndex());
  }

  @Test
  void isScheduled() {
    assertTrue(subject.isScheduled());
  }

  @Test
  void isCanceledOrDeleted() {
    assertFalse(subject.isCanceledOrDeleted());
  }

  @Test
  void isCanceled() {
    assertFalse(subject.isCanceled());
  }

  @Test
  void isDeleted() {
    assertFalse(subject.isDeleted());
  }

  @Test
  void getRealTimeState() {
    assertEquals(RealTimeState.SCHEDULED, subject.getRealTimeState());
  }

  @Test
  void getNumStops() {
    assertEquals(3, subject.getNumStops());
  }

  @Test
  void getWheelchairAccessibility() {
    assertEquals(Accessibility.NO_INFORMATION, subject.getWheelchairAccessibility());
  }

  @Test
  void getOccupancyStatus() {
    assertEquals(OccupancyStatus.NO_DATA_AVAILABLE, subject.getOccupancyStatus(0));
  }

  @Test
  void copyArrivalTimes() {
    assertArrayEquals(new int[] { T10_00, T11_00, T12_00 }, subject.copyArrivalTimes());
  }

  @Test
  void copyDepartureTimes() {
    assertArrayEquals(new int[] { T10_01, T11_02, T12_03 }, subject.copyDepartureTimes());
  }
}
