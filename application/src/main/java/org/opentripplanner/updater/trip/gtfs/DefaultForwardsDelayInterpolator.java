package org.opentripplanner.updater.trip.gtfs;

import org.opentripplanner.transit.model.timetable.RealTimeTripTimesBuilder;
import org.opentripplanner.transit.model.timetable.StopRealTimeState;

/**
 * This interpolator fills in missing times according to the:
 * <a href="https://gtfs.org/documentation/realtime/feed-entities/trip-updates/#stoptimeupdate"> specification</a>
 * <p>
 * If one or more stops are missing along the trip the delay from the update (or, if only time is
 * provided in the update, a delay computed by comparing the time against the GTFS schedule time) is
 * propagated to all subsequent stops.
 * <p>
 * This means that updating a stop time for a certain stop will change all subsequent stops in the
 * absence of any other information. Note that updates with a schedule relationship of SKIPPED will
 * not stop delay propagation, but updates with schedule relationships of SCHEDULED (also the
 * default value if schedule relationship is not provided) or NO_DATA will.
 */
public class DefaultForwardsDelayInterpolator implements ForwardsDelayInterpolator {

  @Override
  public boolean interpolateDelay(RealTimeTripTimesBuilder builder) {
    Integer delay = null;
    Integer time = null;
    StopRealTimeState propagatedState = StopRealTimeState.DEFAULT;
    boolean updated = false;
    for (var i = 0; i < builder.numberOfStops(); ++i) {
      boolean noTimeGiven =
        builder.getArrivalTime(i) == null && builder.getDepartureTime(i) == null;
      if (noTimeGiven) {
        if (builder.getStopRealTimeState(i) == StopRealTimeState.DEFAULT) {
          builder.withStopRealTimeState(i, propagatedState);
        }
      }

      if (builder.getArrivalDelay(i) == null) {
        if (builder.getStopRealTimeState(i) == StopRealTimeState.NO_DATA) {
          // for NO_DATA stops, try to use the scheduled time. However, if the schedule time is
          // earlier than the delayed departure of the previous stop, we cannot set an earlier time
          // than that.
          if (time != null && builder.getScheduledArrivalTime(i) < time) {
            builder.withArrivalTime(i, time);
          } else {
            builder.withArrivalDelay(i, 0);
          }
        } else if (delay != null) {
          // the arrival delay cannot exceed the given departure time
          var departureTime = builder.getDepartureTime(i);
          if (departureTime != null && builder.getScheduledArrivalTime(i) + delay > departureTime) {
            builder.withArrivalTime(i, departureTime);
          } else {
            builder.withArrivalDelay(i, delay);
          }
        }
        updated = true;
      }
      time = builder.getArrivalTime(i);
      delay = builder.getArrivalDelay(i);

      if (builder.getDepartureDelay(i) == null) {
        if (builder.getStopRealTimeState(i) == StopRealTimeState.NO_DATA) {
          // for NO_DATA stops, try to use the scheduled time. However, if the schedule time is
          // earlier than the delayed arrival of this stop, we cannot set an earlier time
          // than that.
          if (time != null && builder.getScheduledDepartureTime(i) < time) {
            builder.withDepartureTime(i, time);
          } else {
            builder.withDepartureDelay(i, 0);
          }
        } else if (delay != null) {
          builder.withDepartureDelay(i, delay);
        }
        updated = true;
      }
      time = builder.getDepartureTime(i);
      delay = builder.getDepartureDelay(i);

      var state = builder.getStopRealTimeState(i);
      // NO_DATA should be propagated per the spec, SKIPPED should not
      // GTFS does not support INACCURATE_PREDICTIONS, but I think it should be propagated as well
      if (state == StopRealTimeState.NO_DATA || state == StopRealTimeState.INACCURATE_PREDICTIONS) {
        propagatedState = state;
      } else {
        propagatedState = StopRealTimeState.DEFAULT;
      }
    }

    // If we reach the end of the trip without a delay value, it means that no estimated times are
    // provided at all (the update may still contain NO_DATA or SKIPPED stops). In such case, copy
    // the scheduled timetable verbatim.
    //
    // If there is a value provided, the backward interpolator should take care of filling in the
    // stops before the first provided value.
    if (delay == null) {
      return builder.copyMissingTimesFromScheduledTimetable();
    }

    return updated;
  }
}
