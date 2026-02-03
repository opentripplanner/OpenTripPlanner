package org.opentripplanner.raptor._data.transit;

import org.opentripplanner.raptor.api.model.RaptorOnBoardAccess;

public class TestRaptorOnBoardAccess implements RaptorOnBoardAccess {

  private final int stop;
  private final int stopPositionInPattern;
  private final int routeIndex;
  private final int tripScheduleIndex;
  private final int generalizedCost;

  public TestRaptorOnBoardAccess(
    int routeIndex,
    int tripScheduleIndex,
    int stopPositionInPattern,
    int stop,
    int generalizedCost
  ) {
    this.routeIndex = routeIndex;
    this.tripScheduleIndex = tripScheduleIndex;
    this.stop = stop;
    this.stopPositionInPattern = stopPositionInPattern;
    this.generalizedCost = generalizedCost;
  }

  @Override
  public int routeIndex() {
    return routeIndex;
  }

  @Override
  public int tripScheduleIndex() {
    return tripScheduleIndex;
  }

  @Override
  public int stopPositionInPattern() {
    return stopPositionInPattern;
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
  public String toString() {
    return asString(true, true, null);
  }
}
