package org.opentripplanner.updater.trip.gtfs;

import java.util.Objects;
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
    Integer firstCanceledStop = null;
    boolean updated = false;
    for (var i = 0; i < builder.numberOfStops(); ++i) {
      boolean noTimeGiven =
        builder.getArrivalTime(i) == null && builder.getDepartureTime(i) == null;
      if (noTimeGiven) {
        if (builder.getStopRealTimeState(i) == StopRealTimeState.DEFAULT) {
          builder.withStopRealTimeState(i, propagatedState);
        }

        if (builder.getStopRealTimeState(i) == StopRealTimeState.CANCELLED) {
          if (firstCanceledStop == null) {
            firstCanceledStop = i;
          }
          continue;
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

      // interpolate time for canceled stops before this stop
      if (firstCanceledStop != null && firstCanceledStop > 0) {
        Integer prevDeparture = builder.getDepartureTime(firstCanceledStop - 1);
        if (prevDeparture != null) {
          int arrival = Objects.requireNonNull(builder.getArrivalTime(i));
          int prevScheduledDeparture = builder.getScheduledDepartureTime(firstCanceledStop - 1);
          int scheduledTravelTime = builder.getScheduledArrivalTime(i) - prevScheduledDeparture;
          int realTimeTravelTime = arrival - prevDeparture;
          double travelTimeRatio = (double) realTimeTravelTime / scheduledTravelTime;

          // Fill out interpolated time for cancelled stops, using the calculated ratio.
          for (int cancelledIndex = firstCanceledStop; cancelledIndex < i; cancelledIndex++) {
            final int scheduledArrivalCancelled = builder.getScheduledArrivalTime(cancelledIndex);
            final int scheduledDepartureCancelled = builder.getScheduledDepartureTime(
              cancelledIndex
            );

            // Interpolate
            int scheduledArrivalDiff = scheduledArrivalCancelled - prevScheduledDeparture;
            double interpolatedArrival = prevDeparture + travelTimeRatio * scheduledArrivalDiff;
            int scheduledDepartureDiff = scheduledDepartureCancelled - prevScheduledDeparture;
            double interpolatedDeparture = prevDeparture + travelTimeRatio * scheduledDepartureDiff;

            // Set Interpolated Times
            builder.withArrivalTime(cancelledIndex, (int) interpolatedArrival);
            builder.withDepartureTime(cancelledIndex, (int) interpolatedDeparture);
            updated = true;
          }
        }
      }
      firstCanceledStop = null;

      var state = builder.getStopRealTimeState(i);
      // NO_DATA should be propagated per the spec, SKIPPED should not
      // GTFS does not support INACCURATE_PREDICTIONS, but I think it should be propagated as well
      if (state == StopRealTimeState.NO_DATA || state == StopRealTimeState.INACCURATE_PREDICTIONS) {
        propagatedState = state;
      } else {
        propagatedState = StopRealTimeState.DEFAULT;
      }
    }

    if (delay != null && firstCanceledStop != null) {
      // there are skipped stops without estimated times at the end of the journey, propagate delay
      for (int i = firstCanceledStop; i < builder.numberOfStops(); ++i) {
        builder.withArrivalDelay(i, delay);
        builder.withDepartureDelay(i, delay);
        updated = true;
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
