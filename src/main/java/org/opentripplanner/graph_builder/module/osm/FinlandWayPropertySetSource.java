package org.opentripplanner.graph_builder.module.osm;

import static org.opentripplanner.graph_builder.module.osm.WayPropertySetSource.DrivingDirection.RIGHT_HAND_TRAFFIC;

import org.opentripplanner.routing.core.intersection_model.IntersectionTraversalCostModel;
import org.opentripplanner.routing.core.intersection_model.NorwayIntersectionTraversalCostModel;
import org.opentripplanner.routing.core.intersection_model.SimpleIntersectionTraversalCostModel;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

/**
 * OSM way properties for Finnish roads. FinlandWayPropertySetSource is derived from NorwayPropertySetSource by seime

 * The main difference compared to the default property set is that most of the highway=trunk roads also allows walking and biking, 
 * where as some does not. 
 * http://wiki.openstreetmap.org/wiki/Tag:highway%3Dtrunk
 * http://wiki.openstreetmap.org/wiki/Highway:International_equivalence
 *
 *
 * @author juusokor
 * @see WayPropertySetSource
 * @see DefaultWayPropertySetSource
 */
public class FinlandWayPropertySetSource implements WayPropertySetSource {

  private final DrivingDirection drivingDirection = RIGHT_HAND_TRAFFIC;

  @Override
  public void populateProperties(WayPropertySet props) {
    // Replace existing matching properties as the logic is that the first statement registered takes precedence over later statements
    props.setProperties("highway=trunk_link", StreetTraversalPermission.ALL, 2.06,
        2.06);
    props.setProperties("highway=trunk", StreetTraversalPermission.ALL, 7.47, 7.47);

    // Don't recommend walking in trunk road tunnels
    props.setProperties("highway=trunk;tunnel=yes", StreetTraversalPermission.CAR, 7.47, 7.47);

    // Do not walk on "moottoriliikennetie"
    props.setProperties("motorroad=yes", StreetTraversalPermission.CAR, 7.47, 7.47);

    // Remove informal and private roads
    props.setProperties("highway=*;informal=yes", StreetTraversalPermission.NONE);
    props.setProperties("highway=service;access=private", StreetTraversalPermission.NONE);
    props.setProperties("highway=trail", StreetTraversalPermission.NONE);

    // No biking on designated footways/sidewalks
    props.setProperties("highway=footway", StreetTraversalPermission.PEDESTRIAN);

    // Prefer designated cycleways
    props.setProperties("highway=cycleway;bicycle=designated",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.6, 0.6);

    // Remove Helsinki city center service tunnel network from graph
    props.setProperties("highway=service;tunnel=yes;access=destination", StreetTraversalPermission.NONE);
    props.setProperties("highway=service;access=destination", StreetTraversalPermission.ALL, 1.1, 1.1);

    /*
     * Automobile speeds in Finland. General speed limit is 80kph unless signs says otherwise.
     *
     */
    props.setCarSpeed("highway=motorway", 27.77f); // = 100kph. Varies between 80 - 120 kph depending on road and season.
    props.setCarSpeed("highway=motorway_link", 15); // = 54kph
    props.setCarSpeed("highway=trunk", 22.22f); // 80kph "Valtatie"
    props.setCarSpeed("highway=trunk_link", 15); // = 54kph
    props.setCarSpeed("highway=primary", 22.22f); // 80kph "Kantatie"
    props.setCarSpeed("highway=primary_link", 15); // = 54kph

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
