package org.opentripplanner.ext.ridehailing;

import java.util.List;
import java.util.concurrent.ExecutionException;
import org.opentripplanner.ext.ridehailing.model.ArrivalTime;
import org.opentripplanner.ext.ridehailing.model.RideEstimate;
import org.opentripplanner.ext.ridehailing.model.RideHailingProvider;
import org.opentripplanner.framework.geometry.WgsCoordinate;

public class FailingRideHailingService implements RideHailingService {

  @Override
  public RideHailingProvider provider() {
    return RideHailingProvider.UBER;
  }

  @Override
  public List<ArrivalTime> arrivalTimes(WgsCoordinate coordinate, boolean b)
    throws ExecutionException {
    throw new ExecutionException(new RuntimeException());
  }

  @Override
  public List<RideEstimate> rideEstimates(WgsCoordinate start, WgsCoordinate end, boolean b)
    throws ExecutionException {
    throw new ExecutionException(new RuntimeException());
  }
}
