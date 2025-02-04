package org.opentripplanner.ext.flex;

import org.opentripplanner._support.geometry.Polygons;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.utils.time.TimeUtils;

public class FlexStopTimesForTest {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();
  private static final StopLocation AREA_STOP = TEST_MODEL
    .areaStop("area")
    .withGeometry(Polygons.BERLIN)
    .build();
  private static final RegularStop REGULAR_STOP = TEST_MODEL.stop("stop").build();

  private static final Trip TRIP = TimetableRepositoryForTest.trip("flex").build();

  public static StopTime area(String startTime, String endTime) {
    return area(AREA_STOP, endTime, startTime);
  }

  public static StopTime area(StopLocation areaStop, String endTime, String startTime) {
    var stopTime = new StopTime();
    stopTime.setStop(areaStop);
    stopTime.setFlexWindowStart(TimeUtils.time(startTime));
    stopTime.setFlexWindowEnd(TimeUtils.time(endTime));
    stopTime.setTrip(TRIP);
    return stopTime;
  }

  /**
   * Returns an invalid combination of a flex area and continuous stopping.
   */
  public static StopTime areaWithContinuousStopping(String time) {
    var st = area(time, time);
    st.setFlexContinuousPickup(PickDrop.COORDINATE_WITH_DRIVER);
    st.setFlexContinuousDropOff(PickDrop.COORDINATE_WITH_DRIVER);
    return st;
  }

  /**
   * Returns an invalid combination of a flex area and continuous pick up.
   */
  public static StopTime areaWithContinuousPickup(String time) {
    var st = area(time, time);
    st.setFlexContinuousPickup(PickDrop.COORDINATE_WITH_DRIVER);
    return st;
  }

  /**
   * Returns an invalid combination of a flex area and continuous drop off.
   */
  public static StopTime areaWithContinuousDropOff(String time) {
    var st = area(time, time);
    st.setFlexContinuousDropOff(PickDrop.COORDINATE_WITH_DRIVER);
    return st;
  }

  public static StopTime regularStop(String arrivalTime, String departureTime) {
    return regularStop(TimeUtils.time(arrivalTime), TimeUtils.time(departureTime));
  }

  public static StopTime regularStop(String time) {
    return regularStop(TimeUtils.time(time), TimeUtils.time(time));
  }

  public static StopTime regularStopWithContinuousStopping(String time) {
    var st = regularStop(TimeUtils.time(time), TimeUtils.time(time));
    st.setFlexContinuousPickup(PickDrop.COORDINATE_WITH_DRIVER);
    st.setFlexContinuousDropOff(PickDrop.COORDINATE_WITH_DRIVER);
    return st;
  }

  public static StopTime regularStopWithContinuousPickup(String time) {
    var st = regularStop(TimeUtils.time(time), TimeUtils.time(time));
    st.setFlexContinuousPickup(PickDrop.COORDINATE_WITH_DRIVER);
    return st;
  }

  public static StopTime regularStopWithContinuousDropOff(String time) {
    var st = regularStop(TimeUtils.time(time), TimeUtils.time(time));
    st.setFlexContinuousDropOff(PickDrop.COORDINATE_WITH_DRIVER);
    return st;
  }

  public static StopTime regularStop(int arrivalTime, int departureTime) {
    var stopTime = new StopTime();
    stopTime.setStop(REGULAR_STOP);
    stopTime.setArrivalTime(arrivalTime);
    stopTime.setDepartureTime(departureTime);
    stopTime.setTrip(TRIP);
    return stopTime;
  }
}
