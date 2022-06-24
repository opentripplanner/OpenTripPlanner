package org.opentripplanner.graph_builder.module.osm;

import static org.opentripplanner.graph_builder.module.osm.WayPropertySetSource.DrivingDirection.RIGHT_HAND_TRAFFIC;

import org.opentripplanner.routing.core.intersection_model.IntersectionTraversalCostModel;
import org.opentripplanner.routing.core.intersection_model.SimpleIntersectionTraversalCostModel;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

/**
 * OSM way properties for Belgium roads
 *
 * Changes:
 * - Allow Bicycle & Pedestrian crossing of ways marked as access:destination
 * See : https://forum.openstreetmap.org/viewtopic.php?id=75840
 *
 * @author thomashermine
 * @see WayPropertySetSource
 * @see DefaultWayPropertySetSource
 */
public class BelgiumWayPropertySetSource implements WayPropertySetSource {

  private final DrivingDirection drivingDirection = RIGHT_HAND_TRAFFIC;

  @Override
  public void populateProperties(WayPropertySet props) {
    props.setProperties("access=destination", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
    // Read the rest from the default set
    new DefaultWayPropertySetSource().populateProperties(props);
  }

  @Override
  public DrivingDirection drivingDirection() {
    return drivingDirection;
  }

  @Override
  public IntersectionTraversalCostModel getIntersectionTraversalCostModel() {
    return new SimpleIntersectionTraversalCostModel(drivingDirection);
  }
}
