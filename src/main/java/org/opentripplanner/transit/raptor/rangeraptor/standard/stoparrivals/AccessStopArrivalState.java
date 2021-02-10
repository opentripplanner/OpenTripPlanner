package org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.util.time.TimeUtils;

public class AccessStopArrivalState<T extends RaptorTripSchedule> extends StopArrivalState<T> {

  private final RaptorTransfer accessPath;

  public AccessStopArrivalState(int time, RaptorTransfer accessPath) {
    this.accessPath = accessPath;
    setAccessTime(time, accessPath.durationInSeconds());
  }

  public AccessStopArrivalState(int time, RaptorTransfer accessPath, StopArrivalState<T> other) {
    super(other);
    this.accessPath = accessPath;
    setAccessTime(time, accessPath.durationInSeconds());
  }

  @Override
  public final boolean arrivedByAccess() {
    return true;
  }

  public RaptorTransfer accessPath() {
    return accessPath;
  }

  @Override
  public String toString() {
    return String.format(
        "Access Arrival { time: %s, duration: %s }",
        TimeUtils.timeToStrLong(time()),
        TimeUtils.timeToStrCompact(accessDuration())
    );
  }
}
