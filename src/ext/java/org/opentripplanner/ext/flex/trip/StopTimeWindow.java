package org.opentripplanner.ext.flex.trip;

import static org.opentripplanner.model.StopTime.MISSING_VALUE;

import java.io.Serializable;
import org.opentripplanner.framework.lang.IntRange;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model.site.StopLocation;

class StopTimeWindow implements Serializable {

  private final StopLocation stop;

  private final int start;
  private final int end;

  private final PickDrop pickupType;
  private final PickDrop dropOffType;

  StopTimeWindow(StopTime st) {
    stop = st.getStop();

    // Fallback to what times are available
    final int earliestPossibleDepartureTime = st.getEarliestPossibleDepartureTime();
    final int latestPossibleArrivalTime = st.getLatestPossibleArrivalTime();

    // We need to make sure that both start and end times are set, if either is set.
    start = getAvailableTime(earliestPossibleDepartureTime, latestPossibleArrivalTime);
    end = getAvailableTime(latestPossibleArrivalTime, earliestPossibleDepartureTime);

    // Do not allow for pickup/dropoff if times are not available
    pickupType = start == MISSING_VALUE ? PickDrop.NONE : st.getPickupType();
    dropOffType = end == MISSING_VALUE ? PickDrop.NONE : st.getDropOffType();
  }

  public StopLocation stop() {
    return stop;
  }

  public int start() {
    return start;
  }

  public int end() {
    return end;
  }

  public PickDrop pickupType() {
    return pickupType;
  }

  public PickDrop dropOffType() {
    return dropOffType;
  }

  public IntRange timeWindow() {
    return IntRange.ofInclusive(start, end);
  }

  private static int getAvailableTime(int... times) {
    for (var time : times) {
      if (time != MISSING_VALUE) {
        return time;
      }
    }

    return MISSING_VALUE;
  }
}
