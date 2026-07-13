package org.opentripplanner.updater.trip.siri;

import javax.annotation.Nullable;

/**
 * Represents a real-time SIRI update for a single stop within a trip, encapsulating both scheduled
 * and real-time arrival and departure times. This class provides the means to calculate arrival
 * and departure delays based on the difference between scheduled and real-time times.
 *
 * A value of {@code null} is used to indicate that a real-time time is missing or unavailable.
 * When real-time data is absent, the scheduled time is used as a fallback. For the first stop
 * in a trip, if the real-time arrival time is missing, the real-time departure time is used
 * as a fallback before resorting to the scheduled arrival time. Similarly, for the last stop,
 * if the real-time departure time is missing, the real-time arrival time is used before
 * falling back to the scheduled departure time.
 */
class StopTimeUpdate {

  private final int scheduledArrivalTime;

  @Nullable
  private final Integer realTimeArrivalTime;

  private final int scheduledDepartureTime;

  @Nullable
  private final Integer realTimeDepartureTime;

  private final boolean firstStop;
  private final boolean lastStop;

  StopTimeUpdate(
    int scheduledArrivalTime,
    @Nullable Integer realTimeArrivalTime,
    int scheduledDepartureTime,
    @Nullable Integer realTimeDepartureTime,
    boolean firstStop,
    boolean lastStop
  ) {
    this.scheduledArrivalTime = scheduledArrivalTime;
    this.realTimeArrivalTime = realTimeArrivalTime;
    this.scheduledDepartureTime = scheduledDepartureTime;
    this.realTimeDepartureTime = realTimeDepartureTime;
    this.firstStop = firstStop;
    this.lastStop = lastStop;
  }

  public boolean hasRealTimeUpdate() {
    return realTimeArrivalTime != null || realTimeDepartureTime != null;
  }

  int getArrivalDelay() {
    return getArrivalTime() - scheduledArrivalTime;
  }

  int getDepartureDelay() {
    return getDepartureTime() - scheduledDepartureTime;
  }

  private int getArrivalTime() {
    return firstStop
      ? handleMissingRealtime(realTimeArrivalTime, realTimeDepartureTime, scheduledArrivalTime)
      : handleMissingRealtime(realTimeArrivalTime, scheduledArrivalTime);
  }

  private int getDepartureTime() {
    return lastStop
      ? handleMissingRealtime(realTimeDepartureTime, realTimeArrivalTime, scheduledDepartureTime)
      : handleMissingRealtime(realTimeDepartureTime, scheduledDepartureTime);
  }

  private static int handleMissingRealtime(@Nullable Integer realtimeTime, int scheduledTime) {
    return realtimeTime != null ? realtimeTime : scheduledTime;
  }

  private static int handleMissingRealtime(
    @Nullable Integer firstRealtimeTime,
    @Nullable Integer secondRealtimeTime,
    int scheduledTime
  ) {
    return firstRealtimeTime != null
      ? firstRealtimeTime
      : handleMissingRealtime(secondRealtimeTime, scheduledTime);
  }
}
