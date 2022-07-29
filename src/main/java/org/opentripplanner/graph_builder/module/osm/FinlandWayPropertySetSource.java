package org.opentripplanner.graph_builder.module.osm;

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
    props.setProperties(
      "highway=living_street",
      new WayPropertiesBuilder(ALL).bicycleSafety(0.9).walkSafety(0.9).build()
    );
    props.setProperties("highway=unclassified", new WayPropertiesBuilder(ALL).build());
    props.setProperties("highway=road", new WayPropertiesBuilder(ALL).build());
    props.setProperties(
      "highway=byway",
      new WayPropertiesBuilder(ALL).bicycleSafety(1.3).walkSafety(1.3).build()
    );
    props.setProperties(
      "highway=track",
      new WayPropertiesBuilder(ALL).bicycleSafety(1.3).walkSafety(1.3).build()
    );
    props.setProperties(
      "highway=service",
      new WayPropertiesBuilder(ALL).bicycleSafety(1.1).walkSafety(1.1).build()
    );
    props.setProperties(
      "highway=residential",
      new WayPropertiesBuilder(ALL).bicycleSafety(0.98).walkSafety(0.98).build()
    );
    props.setProperties(
      "highway=residential_link",
      new WayPropertiesBuilder(ALL).bicycleSafety(0.98).walkSafety(0.98).build()
    );
    props.setProperties("highway=tertiary", new WayPropertiesBuilder(ALL).build());
    props.setProperties("highway=tertiary_link", new WayPropertiesBuilder(ALL).build());
    props.setProperties(
      "highway=secondary",
      new WayPropertiesBuilder(ALL).bicycleSafety(1.5).walkSafety(1.5).build()
    );
    props.setProperties(
      "highway=secondary_link",
      new WayPropertiesBuilder(ALL).bicycleSafety(1.5).walkSafety(1.5).build()
    );
    props.setProperties(
      "highway=primary",
      new WayPropertiesBuilder(ALL).bicycleSafety(2.06).walkSafety(2.06).build()
    );
    props.setProperties(
      "highway=primary_link",
      new WayPropertiesBuilder(ALL).bicycleSafety(2.06).walkSafety(2.06).build()
    );
    // Replace existing matching properties as the logic is that the first statement registered takes precedence over later statements
    props.setProperties(
      "highway=trunk_link",
      new WayPropertiesBuilder(ALL).bicycleSafety(2.06).walkSafety(2.06).build()
    );
    props.setProperties(
      "highway=trunk",
      new WayPropertiesBuilder(ALL).bicycleSafety(7.47).walkSafety(7.47).build()
    );

    // Don't recommend walking in trunk road tunnels
    props.setProperties(
      "highway=trunk;tunnel=yes",
      new WayPropertiesBuilder(CAR).bicycleSafety(7.47).build()
    );

    // Do not walk on "moottoriliikennetie"
    props.setProperties("motorroad=yes", new WayPropertiesBuilder(CAR).bicycleSafety(7.47).build());

    // Remove informal and private roads
    props.setProperties("highway=*;informal=yes", new WayPropertiesBuilder(NONE).build());
    props.setProperties("highway=service;access=private", new WayPropertiesBuilder(NONE).build());
    props.setProperties("highway=trail", new WayPropertiesBuilder(NONE).build());

    // No biking on designated footways/sidewalks
    props.setProperties("highway=footway", new WayPropertiesBuilder(PEDESTRIAN).build());

    // Prefer designated cycleways
    props.setProperties(
      "highway=cycleway;bicycle=designated",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(0.6).build()
    );

    // Remove Helsinki city center service tunnel network from graph
    props.setProperties(
      "highway=service;tunnel=yes;access=destination",
      new WayPropertiesBuilder(NONE).build()
    );
    props.setProperties(
      "highway=service;access=destination",
      new WayPropertiesBuilder(ALL).bicycleSafety(1.1).walkSafety(1.1).build()
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
