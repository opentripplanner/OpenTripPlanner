package org.opentripplanner.ext.ridehailing;

import static org.opentripplanner.ext.ridehailing.model.RideHailingProvider.UBER;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.ext.ridehailing.model.ArrivalTime;
import org.opentripplanner.ext.ridehailing.model.RideEstimate;
import org.opentripplanner.ext.ridehailing.model.RideHailingProvider;
import org.opentripplanner.framework.geometry.WgsCoordinate;

public class TestRideHailingService implements RideHailingService {

  public static final Duration DEFAULT_ARRIVAL_DURATION = Duration.ofMinutes(10);
  public static final List<ArrivalTime> DEFAULT_ARRIVAL_TIMES = List.of(
    new ArrivalTime(UBER, "123", "a ride", DEFAULT_ARRIVAL_DURATION)
  );
  private final List<ArrivalTime> arrivalTimes;
  private final List<RideEstimate> rideEstimates;

  public TestRideHailingService(List<ArrivalTime> arrivalTimes, List<RideEstimate> rideEstimates) {
    this.arrivalTimes = arrivalTimes;
    this.rideEstimates = rideEstimates;
  }

  @Override
  public RideHailingProvider provider() {
    return RideHailingProvider.UBER;
  }

  @Override
  public List<ArrivalTime> arrivalTimes(WgsCoordinate coordinate, boolean wheelchair) {
    return arrivalTimes;
  }

  @Override
  public List<RideEstimate> rideEstimates(
    WgsCoordinate start,
    WgsCoordinate end,
    boolean wheelchair
  ) {
    return rideEstimates;
  }
}
