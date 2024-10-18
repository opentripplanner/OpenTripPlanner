package org.opentripplanner.transit.model.timetable;

import java.util.ArrayList;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.transit.model.network.TripPattern;

public class TripTimesStringBuilder {

  /**
   * This encodes the trip times and information about stops in a readable way in order to simplify
   * testing/debugging. The format of the outputput string is:
   *
   * <pre>
   * REALTIME_STATE | stop1 [FLAGS] arrivalTime departureTime | stop2 ...
   *
   * Where flags are:
   * C: Canceled
   * R: Recorded
   * PI: Prediction Inaccurate
   * ND: No Data
   * </pre>
   *
   * @throws IllegalStateException if TripTimes does not match the TripPattern
   */
  public static String encodeTripTimes(TripTimes tripTimes, TripPattern pattern) {
    var stops = pattern.getStops();

    if (tripTimes.getNumStops() != stops.size()) {
      throw new IllegalArgumentException(
        "TripTimes and TripPattern have different number of stops"
      );
    }

    StringBuilder s = new StringBuilder(tripTimes.getRealTimeState().toString());
    for (int i = 0; i < tripTimes.getNumStops(); i++) {
      var depart = tripTimes.getDepartureTime(i);
      var arrive = tripTimes.getArrivalTime(i);
      var flags = new ArrayList<String>();
      if (tripTimes.isCancelledStop(i)) {
        flags.add("C");
      }
      if (tripTimes.isRecordedStop(i)) {
        flags.add("R");
      }
      if (tripTimes.isPredictionInaccurate(i)) {
        flags.add("PI");
      }
      if (tripTimes.isNoDataStop(i)) {
        flags.add("ND");
      }

      s.append(" | ").append(stops.get(i).getName());
      if (!flags.isEmpty()) {
        s.append(" [").append(String.join(",", flags)).append("]");
      }
      s
        .append(" ")
        .append(TimeUtils.timeToStrCompact(arrive))
        .append(" ")
        .append(TimeUtils.timeToStrCompact(depart));
    }
    return s.toString();
  }
}
