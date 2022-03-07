package org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.util.time.TimeUtils;

class AccessStopArrivalState<T extends RaptorTripSchedule> extends DefaultStopArrivalState<T> {

  AccessStopArrivalState(int time, RaptorTransfer accessPath) {
    setAccessTime(time, accessPath);
  }

  AccessStopArrivalState(int time, RaptorTransfer accessPath, StopArrivalState<T> other) {
    super((DefaultStopArrivalState<T>) other);
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
        TimeUtils.timeToStrCompact(accessPath().durationInSeconds())
    );
  }
}
