package org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.util.time.TimeUtils;

public class AccessStopArrivalState<T extends RaptorTripSchedule> extends StopArrivalState<T> {

  public AccessStopArrivalState(int time, RaptorTransfer accessPath) {
    setAccessTime(time, accessPath);
  }

  public AccessStopArrivalState(int time, RaptorTransfer accessPath, StopArrivalState<T> other) {
    super(other);
    setAccessTime(time, accessPath);
  }

  @Override
  public final boolean arrivedByAccess() {
    return true;
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
