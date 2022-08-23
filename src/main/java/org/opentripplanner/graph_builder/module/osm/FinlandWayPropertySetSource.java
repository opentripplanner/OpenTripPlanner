package org.opentripplanner.graph_builder.module.osm;

import static org.opentripplanner.graph_builder.module.osm.WayPropertiesBuilder.withModes;
import static org.opentripplanner.graph_builder.module.osm.WayPropertySetSource.DrivingDirection.RIGHT_HAND_TRAFFIC;
import static org.opentripplanner.routing.edgetype.StreetTraversalPermission.ALL;
import static org.opentripplanner.routing.edgetype.StreetTraversalPermission.CAR;
import static org.opentripplanner.routing.edgetype.StreetTraversalPermission.NONE;
import static org.opentripplanner.routing.edgetype.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.routing.edgetype.StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;

import org.opentripplanner.routing.core.intersection_model.IntersectionTraversalCostModel;
import org.opentripplanner.routing.core.intersection_model.SimpleIntersectionTraversalCostModel;

/**
 * OSM way properties for Finnish roads. FinlandWayPropertySetSource is derived from
 * NorwayPropertySetSource by seime
 * <p>
 * The main difference compared to the default property set is that most of the highway=trunk roads
 * also allows walking and biking, where as some does not. http://wiki.openstreetmap.org/wiki/Tag:highway%3Dtrunk
 * http://wiki.openstreetmap.org/wiki/Highway:International_equivalence
 *
 * @author juusokor
 * @see WayPropertySetSource
 * @see DefaultWayPropertySetSource
 */
public class FinlandWayPropertySetSource implements WayPropertySetSource {

  private final DrivingDirection drivingDirection = RIGHT_HAND_TRAFFIC;

  @Override
  public void populateProperties(WayPropertySet props) {
    props.setProperties("highway=living_street", withModes(ALL).bicycleSafety(0.9).walkSafety(0.9));
    props.setProperties("highway=unclassified", withModes(ALL));
    props.setProperties("highway=road", withModes(ALL));
    props.setProperties("highway=byway", withModes(ALL).bicycleSafety(1.3).walkSafety(1.3));
    props.setProperties("highway=track", withModes(ALL).bicycleSafety(1.3).walkSafety(1.3));
    props.setProperties("highway=service", withModes(ALL).bicycleSafety(1.1).walkSafety(1.1));
    props.setProperties("highway=residential", withModes(ALL).bicycleSafety(0.98).walkSafety(0.98));
    props.setProperties(
      "highway=residential_link",
      withModes(ALL).bicycleSafety(0.98).walkSafety(0.98)
    );
    props.setProperties("highway=tertiary", withModes(ALL));
    props.setProperties("highway=tertiary_link", withModes(ALL));
    props.setProperties("highway=secondary", withModes(ALL).bicycleSafety(1.5).walkSafety(1.5));
    props.setProperties(
      "highway=secondary_link",
      withModes(ALL).bicycleSafety(1.5).walkSafety(1.5)
    );
    props.setProperties("highway=primary", withModes(ALL).bicycleSafety(2.06).walkSafety(2.06));
    props.setProperties(
      "highway=primary_link",
      withModes(ALL).bicycleSafety(2.06).walkSafety(2.06)
    );
    // Replace existing matching properties as the logic is that the first statement registered takes precedence over later statements
    props.setProperties("highway=trunk_link", withModes(ALL).bicycleSafety(2.06).walkSafety(2.06));
    props.setProperties("highway=trunk", withModes(ALL).bicycleSafety(7.47).walkSafety(7.47));

    // Don't recommend walking in trunk road tunnels
    props.setProperties("highway=trunk;tunnel=yes", withModes(CAR).bicycleSafety(7.47));

    // Do not walk on "moottoriliikennetie"
    props.setProperties("motorroad=yes", withModes(CAR).bicycleSafety(7.47));

    // Remove informal and private roads
    props.setProperties("highway=*;informal=yes", withModes(NONE));
    props.setProperties("highway=service;access=private", withModes(NONE));
    props.setProperties("highway=trail", withModes(NONE));

    // No biking on designated footways/sidewalks
    props.setProperties("highway=footway", withModes(PEDESTRIAN));

    // Prefer designated cycleways
    props.setProperties(
      "highway=cycleway;bicycle=designated",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(0.6)
    );

    // Remove Helsinki city center service tunnel network from graph
    props.setProperties("highway=service;tunnel=yes;access=destination", withModes(NONE));
    props.setProperties(
      "highway=service;access=destination",
      withModes(ALL).bicycleSafety(1.1).walkSafety(1.1)
    );

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
