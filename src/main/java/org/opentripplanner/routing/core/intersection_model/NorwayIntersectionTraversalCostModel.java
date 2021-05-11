package org.opentripplanner.routing.core.intersection_model;

import org.opentripplanner.graph_builder.module.osm.WayPropertySetSource;

public class NorwayIntersectionTraversalCostModel extends SimpleIntersectionTraversalCostModel{

  public NorwayIntersectionTraversalCostModel(
      WayPropertySetSource.DrivingDirection drivingDirection
  ) {
    super(drivingDirection);
  }

  @Override
  public double getExpectedStraightNoLightTimeSec() {
    return 0.0;
  }
}
