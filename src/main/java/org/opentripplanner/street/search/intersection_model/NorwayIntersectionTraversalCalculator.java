package org.opentripplanner.street.search.intersection_model;

public class NorwayIntersectionTraversalCalculator extends SimpleIntersectionTraversalCalculator {

  public NorwayIntersectionTraversalCalculator(DrivingDirection drivingDirection) {
    super(drivingDirection);
  }

  @Override
  public double getExpectedStraightNoLightTimeSec() {
    return 0.0;
  }
}
