package org.opentripplanner.graph_builder.module.osm;

import static org.opentripplanner.graph_builder.module.osm.WayPropertiesBuilder.withModes;
import static org.opentripplanner.routing.edgetype.StreetTraversalPermission.ALL;

import org.opentripplanner.routing.core.intersection_model.IntersectionTraversalCostModel;
import org.opentripplanner.routing.core.intersection_model.SimpleIntersectionTraversalCostModel;

/**
 * OSM way properties for the Atlanta, Georgia, USA area.
 * The differences compared to the default
 * property set are: In Atlanta "trunk" is used for the most important primary thoroughfares, but
 * these roads typically still allow pedestrian traffic and often include bus service / stops.
 *
 * @author demory
 * @see WayPropertySetSource
 * @see DefaultWayPropertySetSource
 */

public class AtlantaWayPropertySetSource implements WayPropertySetSource {

  private static final DrivingDirection drivingDirection = DrivingDirection.RIGHT_HAND_TRAFFIC;

  @Override
  public void populateProperties(WayPropertySet props) {
    // Replace existing matching properties as the logic is that the first statement registered takes precedence over later statements
    props.setProperties("highway=trunk_link", withModes(ALL).bicycleSafety(2.5));
    props.setProperties("highway=trunk", withModes(ALL).bicycleSafety(2.5));

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
