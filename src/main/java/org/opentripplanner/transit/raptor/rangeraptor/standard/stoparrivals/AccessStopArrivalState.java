package org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.util.TimeUtils;

public class AccessStopArrivalState<T extends RaptorTripSchedule> extends StopArrivalState<T> {

  private RaptorTransfer access;

  public RaptorTransfer access() {
    return access;
  }

  void setAccess(int time, RaptorTransfer access) {
    super.setAccessTime(time, access.durationInSeconds());
    this.access = access;
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
