package org.opentripplanner.graph_builder.module.osm;

import static org.opentripplanner.graph_builder.module.osm.WayPropertySetSource.DrivingDirection.RIGHT_HAND_TRAFFIC;
import static org.opentripplanner.routing.edgetype.StreetTraversalPermission.ALL;
import static org.opentripplanner.routing.edgetype.StreetTraversalPermission.BICYCLE_AND_CAR;
import static org.opentripplanner.routing.edgetype.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.routing.edgetype.StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;

import org.opentripplanner.routing.core.intersection_model.IntersectionTraversalCostModel;
import org.opentripplanner.routing.core.intersection_model.SimpleIntersectionTraversalCostModel;

/**
 * OSM way properties for German roads. Speed limits where adjusted to German regulation and some
 * bike safety settings tweaked, especially including tracktype's grade and preference of bicycle
 * networks.
 *
 * @see WayPropertySetSource
 * @see DefaultWayPropertySetSource
 */
public class GermanyWayPropertySetSource implements WayPropertySetSource {

  private final DrivingDirection drivingDirection = RIGHT_HAND_TRAFFIC;

  @Override
  public void populateProperties(WayPropertySet props) {
    // Replace existing matching properties as the logic is that the first statement registered takes precedence over later statements

    // Automobile speeds in Germany. General speed limit is 50kph in settlements, 100kph outside settlements.
    // For motorways, there (currently still) is no limit. Nevertheless 120kph is assumed to reflect varying
    // traffic conditions.
    props.setCarSpeed("highway=motorway", 33.33f); // = 120kph. Varies between 80 - 120 kph depending on road and season.
    props.setCarSpeed("highway=motorway_link", 15); // = 54kph
    props.setCarSpeed("highway=trunk", 27.27f); // 100kph
    props.setCarSpeed("highway=trunk_link", 15); // = 54kph
    props.setCarSpeed("highway=primary", 27.27f); // 100kph
    props.setCarSpeed("highway=primary_link", 15); // = 54kph

    // you should only use parking aisle if there is no other options
    // ideally they would be set to noThruTraffic but that would mean the parking lots are inaccessible
    props.setCarSpeed("service=parking_aisle", 5);

    // Many agricultural ways are tagged as 'track' but have no access tags. We assume this to mean that cars
    // are prohibited.
    // https://www.openstreetmap.org/way/124263424
    props.setProperties("highway=track", new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).build());
    props.setProperties(
      "highway=track;surface=*",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).build()
    );

    props.setProperties(
      "highway=residential;junction=roundabout",
      new WayPropertiesBuilder(ALL).bicycleSafety(0.98).build()
    );
    props.setProperties(
      "highway=*;junction=roundabout",
      new WayPropertiesBuilder(BICYCLE_AND_CAR).build()
    );

    // Pedestrian zones in Germany are forbidden for bicycles by default
    props.setProperties("highway=pedestrian", new WayPropertiesBuilder(PEDESTRIAN).build());
    props.setProperties(
      "highway=residential;maxspeed=30",
      new WayPropertiesBuilder(ALL).bicycleSafety(0.9).build()
    );
    props.setProperties(
      "highway=footway;bicycle=yes",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(0.8).build()
    );
    // Default was 2.5, we want to favor using mixed footways somewhat
    props.setProperties(
      "footway=sidewalk;highway=footway;bicycle=yes",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.2).build()
    );

    props.setMixinProperties(
      "highway=tertiary",
      new WayPropertiesBuilder(ALL).bicycleSafety(1.2).build()
    );
    props.setMixinProperties(
      "maxspeed=70",
      new WayPropertiesBuilder(ALL).bicycleSafety(1.5).build()
    );
    props.setMixinProperties("maxspeed=80", new WayPropertiesBuilder(ALL).bicycleSafety(2).build());
    props.setMixinProperties("maxspeed=90", new WayPropertiesBuilder(ALL).bicycleSafety(3).build());
    props.setMixinProperties(
      "maxspeed=100",
      new WayPropertiesBuilder(ALL).bicycleSafety(5).build()
    );

    // tracktypes

    // solid
    props.setMixinProperties("tracktype=grade1", new WayPropertiesBuilder(ALL).build());
    // solid but unpaved
    props.setMixinProperties(
      "tracktype=grade2",
      new WayPropertiesBuilder(ALL).bicycleSafety(1.1).build()
    );
    // mostly solid.
    props.setMixinProperties(
      "tracktype=grade3",
      new WayPropertiesBuilder(ALL).bicycleSafety(1.15).build()
    );
    // mostly soft
    props.setMixinProperties(
      "tracktype=grade4",
      new WayPropertiesBuilder(ALL).bicycleSafety(1.3).build()
    );
    // soft
    props.setMixinProperties(
      "tracktype=grade5",
      new WayPropertiesBuilder(ALL).bicycleSafety(1.5).build()
    );

    // lit=yes currently is tagged very rarely, so we just want to discount where lit=no explicitly
    // not lit decreases safety
    props.setMixinProperties("lit=no", new WayPropertiesBuilder(ALL).bicycleSafety(1.05).build());

    props.setProperties(
      "highway=unclassified;cycleway=lane",
      new WayPropertiesBuilder(ALL).bicycleSafety(0.87).build()
    );

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
