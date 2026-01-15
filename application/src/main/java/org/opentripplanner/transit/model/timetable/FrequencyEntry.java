package org.opentripplanner.transit.model.timetable;

import java.io.Serializable;
import java.time.Duration;
import org.opentripplanner.model.Frequency;
import org.opentripplanner.utils.time.TimeUtils;

/**
 * Uses a TripTimes to represent multiple trips following the same template at regular intervals.
 * (see GTFS frequencies.txt)
 * <p>
 * Refactor this to inherit {@link TripTimes}
 */
public class FrequencyEntry implements Serializable {

  private final int startTime;
  private final int endTime;
  private final int headway_s;
  private final boolean exactTimes;
  private final ScheduledTripTimes tripTimes;

  public FrequencyEntry(Frequency freq, ScheduledTripTimes tripTimes) {
    this.startTime = freq.startTime();
    this.endTime = freq.endTime();
    this.headway_s = freq.headwaySecs();
    this.exactTimes = freq.exactTimes();
    this.tripTimes = tripTimes;
  }

  public FrequencyEntry(
    int startTime,
    int endTime,
    int headway_s,
    boolean exactTimes,
    ScheduledTripTimes tripTimes
  ) {
    this.startTime = startTime;
    this.endTime = endTime;
    this.headway_s = headway_s;
    this.exactTimes = exactTimes;
    this.tripTimes = tripTimes;
  }

  /// Return seconds after midnight (relative to service date)
  public int startTime() {
    return startTime;
  }

  /// Return seconds after midnight (relative to service date)
  public int endTime() {
    return endTime;
  }

  /// Compute the needed slack to make the boarding safe. For true frequency-based trips (not exact
  /// times), we must add the whole headway, since you might miss the previous trip by 1 second. In
  /// areas where most services operate like this, this becomes a bit defensive, and using the
  /// average + a small slack would be better depending on the customer needs - needing an accurate
  /// guaranteed time or a good estimate for the arrival (not supported by OTP yet).
  public int routingSlack() {
    return exactTimes ? 0 : headway_s;
  }

  public ScheduledTripTimes tripTimes() {
    return tripTimes;
  }

  /**
   * Time-shift all times. This is used when updating the time zone for the trip.
   */
  public FrequencyEntry withAdjustedTimes(Duration timeshift) {
    // TODO: The start and end time is NOT adjusted, this looks like it could be a bug, please
    //       explain why this is correct or fix the error. The timeShift operation might be
    //       valid with respect to routing, but the start/end is exposed outside the class
    //       and used in logic which assumes they are in the internal model time-zone.
    return new FrequencyEntry(
      startTime,
      endTime,
      headway_s,
      exactTimes,
      tripTimes.withAdjustedTimes(timeshift)
    );
  }

  /*
        The TripTimes getDepartureTime / getArrivalTime methods do not care when the search is happening.
        The Frequency equivalents need to know when the search is happening, and need to be able to say
        no trip is possible. Therefore we need to add another specialized method.

        Fortunately all uses of the TripTimes itself in traversing edges use relative times,
        so we can fall back on the underlying TripTimes.
     */

  @Override
  public String toString() {
    return String.format(
      "FreqEntry: trip %s start %s end %s headway %s",
      tripTimes.getTrip(),
      TimeUtils.timeToStrLong(startTime),
      TimeUtils.timeToStrLong(endTime),
      TimeUtils.timeToStrLong(headway_s)
    );
  }

  public int nextDepartureTime(int stop, int time) {
    // Start time and end time are for the first stop in the trip. Find the time offset for this stop.
    int stopOffset = tripTimes.getDepartureTime(stop) - tripTimes.getDepartureTime(0);
    // First time a vehicle passes by this stop.
    int beg = startTime + stopOffset;
    // Latest a vehicle can pass by this stop.
    int end = endTime + stopOffset;
    if (time > end) {
      return -1;
    }
    if (exactTimes) {
      for (int dep = beg; dep < end; dep += headway_s) {
        if (dep >= time) {
          return dep;
        }
      }
    } else {
      int dep = time + headway_s;
      // TODO it might work better to step forward until in range
      // this would work better for time window edges.
      if (dep < beg) {
        return beg;
      }
      // not quite right
      if (dep < end) {
        return dep;
      }
    }
    return -1;
  }

  public int prevArrivalTime(int stop, int t) {
    int stopOffset = tripTimes.getArrivalTime(stop) - tripTimes.getDepartureTime(0);
    // First time a vehicle passes by this stop.
    int beg = startTime + stopOffset;
    // Latest a vehicle can pass by this stop.
    int end = endTime + stopOffset;
    if (t < beg) {
      return -1;
    }
    if (exactTimes) {
      // we can't start from end in case end - beg is not a multiple of headway
      int arr;
      for (arr = beg + headway_s; arr < end; arr += headway_s) {
        if (arr > t) {
          return arr - headway_s;
        }
      }
      // if t > end, return last valid arrival time
      return arr - headway_s;
    } else {
      int dep = t - headway_s;
      if (dep > end) {
        return end;
      }
      // not quite right
      if (dep > beg) {
        return dep;
      }
    }
    return -1;
  }

  /**
   * Returns a disposable TripTimes for this frequency entry in which the vehicle passes the given
   * stop index (not stop sequence number) at the given time. This allows us to separate the
   * departure/arrival search process from actually instantiating a TripTimes, to avoid making too
   * many short-lived clones. This delegation is a sign that maybe FrequencyEntry should implement
   * TripTimes.
   */
  public ScheduledTripTimes materialize(int stop, int time, boolean depart) {
    return tripTimes.timeShift(stop, time, depart);
  }

  public FrequencyEntry withServiceCode(int serviceCode) {
    return new FrequencyEntry(
      startTime,
      endTime,
      headway_s,
      exactTimes,
      tripTimes.withServiceCode(serviceCode)
    );
  }
}
