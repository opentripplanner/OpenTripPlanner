package org.opentripplanner.ext.flex;

import static org.opentripplanner.model.StopTime.MISSING_VALUE;

import org.opentripplanner._support.geometry.Polygons;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;

public class FlexStopTimesForTest {

  private static final TransitModelForTest TEST_MODEL = TransitModelForTest.of();
  private static final StopLocation AREA_STOP = TEST_MODEL
    .areaStop("area")
    .withGeometry(Polygons.BERLIN)
    .build();
  private static final RegularStop REGULAR_STOP = TEST_MODEL.stop("stop").build();

  public static StopTime area(String startTime, String endTime) {
    return area(AREA_STOP, endTime, startTime);
  }

  public static StopTime area(StopLocation areaStop, String endTime, String startTime) {
    var stopTime = new StopTime();
    stopTime.setStop(areaStop);
    stopTime.setFlexWindowStart(TimeUtils.time(startTime));
    stopTime.setFlexWindowEnd(TimeUtils.time(endTime));
    return stopTime;
  }

  public static StopTime regularArrival(String arrivalTime) {
    return regularStopTime(TimeUtils.time(arrivalTime), MISSING_VALUE);
  }

  public static StopTime regularStopTime(String arrivalTime, String departureTime) {
    return regularStopTime(TimeUtils.time(arrivalTime), TimeUtils.time(departureTime));
  }

  public static StopTime regularStopTime(int arrivalTime, int departureTime) {
    var stopTime = new StopTime();
    stopTime.setStop(REGULAR_STOP);
    stopTime.setArrivalTime(arrivalTime);
    stopTime.setDepartureTime(departureTime);
    return stopTime;
  }

  public static StopTime regularDeparture(String departureTime) {
    return regularStopTime(MISSING_VALUE, TimeUtils.time(departureTime));
  }
}
