package org.opentripplanner.raptor._data.transit;

import org.opentripplanner.raptor.api.model.RaptorStartOnBoardAccess;
import org.opentripplanner.raptor.api.model.RaptorTripScheduleStopPosition;

public class TestRaptorStartOnBoardAccess implements RaptorStartOnBoardAccess {

  private final int stop;
  private final int generalizedCost;
  private final RaptorTripScheduleStopPosition tripBoarding;

  public TestRaptorStartOnBoardAccess(
    int routeIndex,
    int tripScheduleIndex,
    int stopPositionInPattern,
    int stop,
    int generalizedCost
  ) {
    this.tripBoarding = new RaptorTripScheduleStopPosition(
      routeIndex,
      tripScheduleIndex,
      stopPositionInPattern
    );
    this.stop = stop;
    this.generalizedCost = generalizedCost;
  }

  @Override
  public int stop() {
    return stop;
  }

  @Override
  public int c1() {
    return generalizedCost;
  }

  @Override
  public RaptorTripScheduleStopPosition tripBoarding() {
    return tripBoarding;
  }

  @Override
  public String toString() {
    return asString(true, true, null);
  }
}
