package org.opentripplanner.routing.core.intersection_model;

import org.opentripplanner.graph_builder.module.osm.WayPropertySetSource;

public class NorwayIntersectionTraversalCostModel extends SimpleIntersectionTraversalCostModel{

  private final double expectedStraightNoLightTimeSec = 0.0;

  public NorwayIntersectionTraversalCostModel(
      WayPropertySetSource.DrivingDirection drivingDirection
  ) {
    super(drivingDirection);
  }

  @Override
  public double getExpectedStraightNoLightTimeSec() {
    return expectedStraightNoLightTimeSec;
  }
}
