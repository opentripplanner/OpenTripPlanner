package org.opentripplanner.graph_builder.module.osm;

import static org.opentripplanner.graph_builder.module.osm.WayPropertySetSource.DrivingDirection.RIGHT_HAND_TRAFFIC;
import static org.opentripplanner.routing.edgetype.StreetTraversalPermission.ALL;
import static org.opentripplanner.routing.edgetype.StreetTraversalPermission.BICYCLE;
import static org.opentripplanner.routing.edgetype.StreetTraversalPermission.BICYCLE_AND_CAR;
import static org.opentripplanner.routing.edgetype.StreetTraversalPermission.CAR;
import static org.opentripplanner.routing.edgetype.StreetTraversalPermission.NONE;
import static org.opentripplanner.routing.edgetype.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.routing.edgetype.StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;

import org.opentripplanner.routing.core.intersection_model.IntersectionTraversalCostModel;
import org.opentripplanner.routing.core.intersection_model.NorwayIntersectionTraversalCostModel;

/**
 * OSM way properties for Norwegian roads. The main difference compared to the default property set
 * is that most of the highway=trunk roads also allows walking and biking, where as some does not.
 * http://wiki.openstreetmap.org/wiki/Tag:highway%3Dtrunk http://wiki.openstreetmap.org/wiki/Highway:International_equivalence
 *
 * @author seime
 * @see WayPropertySetSource
 * @see DefaultWayPropertySetSource
 */
public class NorwayWayPropertySetSource implements WayPropertySetSource {

  private final DrivingDirection drivingDirection = RIGHT_HAND_TRAFFIC;

  @Override
  public void populateProperties(WayPropertySet props) {
    var very_high_traffic = 8;
    var high_traffic = 2.25;
    var medium_high_traffic = 2.06;
    var medium_traffic = 1.5;
    var medium_low_traffic = 1.42;
    var low_traffic = 1.1;
    var very_low_traffic = 0.94;

    var cycle_lane_medium_traffic = 0.76;
    var cycle_lane_low_traffic = 0.66;

    var dedicated_footway = 0.85;
    var sidewalk = 1.16;
    var footway_crossing = 1.4;
    var mixed_cycleway = 0.67;
    var dedicated_cycleway = 0.62;
    var dual_lane_or_oneway_cycleway = 0.6;

    props.setProperties("highway=motorway", new WayPropertiesBuilder(CAR).build());
    props.setProperties("highway=motorway_link", new WayPropertiesBuilder(CAR).build());

    // Do not walk on "Motortrafikkvei" ("motorvei klasse b")
    props.setProperties("highway=trunk;motorroad=yes", new WayPropertiesBuilder(CAR).build());
    props.setProperties("highway=trunk_link;motorroad=yes", new WayPropertiesBuilder(CAR).build());

    props.setProperties("highway=primary;motorroad=yes", new WayPropertiesBuilder(CAR).build());
    props.setProperties(
      "highway=primary_link;motorroad=yes",
      new WayPropertiesBuilder(CAR).build()
    );

    /* "highway=trunk" roads ("Riksveier") are often single carriageway, and not only legal for bicycles,
    but assumed to be  much safer than the  default profile (Except when the speedlimt exceeds 90). */
    props.setProperties(
      "highway=trunk",
      new WayPropertiesBuilder(ALL).bicycleSafety(high_traffic).build()
    );
    props.setProperties(
      "highway=trunk_link",
      new WayPropertiesBuilder(ALL).bicycleSafety(high_traffic).build()
    );
    // Discourage cycling on roads with no infrastructure for neither walking nor cycling
    props.setProperties(
      "highway=trunk;foot=no",
      new WayPropertiesBuilder(BICYCLE_AND_CAR).bicycleSafety(very_high_traffic).build()
    );
    props.setProperties(
      "highway=trunk_link;foot=no",
      new WayPropertiesBuilder(BICYCLE_AND_CAR).bicycleSafety(very_high_traffic).build()
    );
    // Discourage cycling on trunk road tunnels
    props.setProperties(
      "highway=trunk;tunnel=yes",
      new WayPropertiesBuilder(CAR).bicycleSafety(very_high_traffic).build()
    );
    props.setProperties(
      "highway=trunk_link;tunnel=yes",
      new WayPropertiesBuilder(CAR).bicycleSafety(very_high_traffic).build()
    );
    props.setProperties(
      "highway=trunk;maxspeed=90",
      new WayPropertiesBuilder(ALL).bicycleSafety(very_high_traffic).build()
    );
    props.setProperties(
      "highway=trunk_link;maxspeed=90",
      new WayPropertiesBuilder(ALL).bicycleSafety(very_high_traffic).build()
    );
    props.setProperties(
      "highway=trunk;maxspeed=60",
      new WayPropertiesBuilder(ALL).bicycleSafety(medium_high_traffic).build()
    );
    props.setProperties(
      "highway=trunk_link;maxspeed=60",
      new WayPropertiesBuilder(ALL).bicycleSafety(medium_high_traffic).build()
    );
    props.setProperties(
      "highway=trunk;maxspeed=50",
      new WayPropertiesBuilder(ALL).bicycleSafety(medium_traffic).build()
    );
    props.setProperties(
      "highway=trunk_link;maxspeed=50",
      new WayPropertiesBuilder(ALL).bicycleSafety(medium_traffic).build()
    );
    props.setProperties(
      "highway=trunk;maxspeed=40",
      new WayPropertiesBuilder(ALL).bicycleSafety(medium_traffic).build()
    );
    props.setProperties(
      "highway=trunk_link;maxspeed=40",
      new WayPropertiesBuilder(ALL).bicycleSafety(medium_traffic).build()
    );

    props.setProperties(
      "highway=primary",
      new WayPropertiesBuilder(ALL).bicycleSafety(high_traffic).build()
    );
    props.setProperties(
      "highway=primary_link",
      new WayPropertiesBuilder(ALL).bicycleSafety(high_traffic).build()
    );
    // Discourage cycling on roads with no infrastructure for neither walking nor cycling
    props.setProperties(
      "highway=primary;foot=no",
      new WayPropertiesBuilder(BICYCLE_AND_CAR).bicycleSafety(very_high_traffic).build()
    );
    props.setProperties(
      "highway=primary_link;foot=no",
      new WayPropertiesBuilder(BICYCLE_AND_CAR).bicycleSafety(very_high_traffic).build()
    );
    props.setProperties(
      "highway=primary;maxspeed=90",
      new WayPropertiesBuilder(ALL).bicycleSafety(very_high_traffic).build()
    );
    props.setProperties(
      "highway=primary_link;maxspeed=90",
      new WayPropertiesBuilder(ALL).bicycleSafety(very_high_traffic).build()
    );
    props.setProperties(
      "highway=primary;maxspeed=60",
      new WayPropertiesBuilder(ALL).bicycleSafety(medium_high_traffic).build()
    );
    props.setProperties(
      "highway=primary_link;maxspeed=60",
      new WayPropertiesBuilder(ALL).bicycleSafety(medium_high_traffic).build()
    );
    props.setProperties(
      "highway=primary;maxspeed=50",
      new WayPropertiesBuilder(ALL).bicycleSafety(medium_traffic).build()
    );
    props.setProperties(
      "highway=primary_link;maxspeed=50",
      new WayPropertiesBuilder(ALL).bicycleSafety(medium_traffic).build()
    );
    props.setProperties(
      "highway=primary;maxspeed=40",
      new WayPropertiesBuilder(ALL).bicycleSafety(medium_traffic).build()
    );
    props.setProperties(
      "highway=primary_link;maxspeed=40",
      new WayPropertiesBuilder(ALL).bicycleSafety(medium_traffic).build()
    );
    props.setProperties(
      "highway=primary;maxspeed=30",
      new WayPropertiesBuilder(ALL).bicycleSafety(low_traffic).build()
    );
    props.setProperties(
      "highway=primary_link;maxspeed=30",
      new WayPropertiesBuilder(ALL).bicycleSafety(low_traffic).build()
    );

    props.setProperties(
      "highway=secondary",
      new WayPropertiesBuilder(ALL).bicycleSafety(medium_high_traffic).build()
    );
    props.setProperties(
      "highway=secondary_link",
      new WayPropertiesBuilder(ALL).bicycleSafety(medium_high_traffic).build()
    );
    // Discourage cycling on roads with no infrastructure for neither walking nor cycling
    props.setProperties(
      "highway=secondary;foot=no",
      new WayPropertiesBuilder(BICYCLE_AND_CAR).bicycleSafety(very_high_traffic).build()
    );
    props.setProperties(
      "highway=secondary_link;foot=no",
      new WayPropertiesBuilder(BICYCLE_AND_CAR).bicycleSafety(very_high_traffic).build()
    );
    props.setProperties(
      "highway=secondary;maxspeed=60",
      new WayPropertiesBuilder(ALL).bicycleSafety(medium_traffic).build()
    );
    props.setProperties(
      "highway=secondary_link;maxspeed=60",
      new WayPropertiesBuilder(ALL).bicycleSafety(medium_traffic).build()
    );
    props.setProperties(
      "highway=secondary;maxspeed=50",
      new WayPropertiesBuilder(ALL).bicycleSafety(medium_low_traffic).build()
    );
    props.setProperties(
      "highway=secondary_link;maxspeed=50",
      new WayPropertiesBuilder(ALL).bicycleSafety(medium_low_traffic).build()
    );
    props.setProperties(
      "highway=secondary;maxspeed=40",
      new WayPropertiesBuilder(ALL).bicycleSafety(medium_low_traffic).build()
    );
    props.setProperties(
      "highway=secondary_link;maxspeed=40",
      new WayPropertiesBuilder(ALL).bicycleSafety(medium_low_traffic).build()
    );
    props.setProperties(
      "highway=secondary;maxspeed=30",
      new WayPropertiesBuilder(ALL).bicycleSafety(low_traffic).build()
    );
    props.setProperties(
      "highway=secondary_link;maxspeed=30",
      new WayPropertiesBuilder(ALL).bicycleSafety(low_traffic).build()
    );

    props.setProperties(
      "highway=tertiary",
      new WayPropertiesBuilder(ALL).bicycleSafety(medium_low_traffic).build()
    );
    props.setProperties(
      "highway=tertiary_link",
      new WayPropertiesBuilder(ALL).bicycleSafety(medium_low_traffic).build()
    );
    // Discourage cycling on roads with no infrastructure for neither walking nor cycling
    props.setProperties(
      "highway=tertiary;foot=no",
      new WayPropertiesBuilder(BICYCLE_AND_CAR).bicycleSafety(very_high_traffic).build()
    );
    props.setProperties(
      "highway=tertiary_link;foot=no",
      new WayPropertiesBuilder(BICYCLE_AND_CAR).bicycleSafety(very_high_traffic).build()
    );
    props.setProperties(
      "highway=tertiary;maxspeed=80",
      new WayPropertiesBuilder(ALL).bicycleSafety(medium_high_traffic).build()
    );
    props.setProperties(
      "highway=tertiary_link;maxspeed=80",
      new WayPropertiesBuilder(ALL).bicycleSafety(medium_high_traffic).build()
    );
    props.setProperties(
      "highway=tertiary;maxspeed=70",
      new WayPropertiesBuilder(ALL).bicycleSafety(medium_high_traffic).build()
    );
    props.setProperties(
      "highway=tertiary_link;maxspeed=70",
      new WayPropertiesBuilder(ALL).bicycleSafety(medium_high_traffic).build()
    );
    props.setProperties(
      "highway=tertiary;maxspeed=40",
      new WayPropertiesBuilder(ALL).bicycleSafety(low_traffic).build()
    );
    props.setProperties(
      "highway=tertiary_link;maxspeed=40",
      new WayPropertiesBuilder(ALL).bicycleSafety(low_traffic).build()
    );
    props.setProperties(
      "highway=tertiary;maxspeed=30",
      new WayPropertiesBuilder(ALL).bicycleSafety(low_traffic).build()
    );
    props.setProperties(
      "highway=tertiary_link;maxspeed=30",
      new WayPropertiesBuilder(ALL).bicycleSafety(low_traffic).build()
    );

    props.setProperties(
      "highway=unclassified",
      new WayPropertiesBuilder(ALL).bicycleSafety(low_traffic).build()
    );
    // Discourage cycling on roads with no infrastructure for neither walking nor cycling
    props.setProperties(
      "highway=unclassified;foot=no",
      new WayPropertiesBuilder(BICYCLE_AND_CAR).bicycleSafety(medium_traffic).build()
    );
    // These access tags indicates low traffic
    props.setProperties(
      "highway=unclassified;motor_vehicle=no",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic).build()
    );
    props.setProperties(
      "highway=unclassified;motor_vehicle=private",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic).build()
    );
    props.setProperties(
      "highway=unclassified;motor_vehicle=permit",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic).build()
    );
    props.setProperties(
      "highway=unclassified;motor_vehicle=destination",
      new WayPropertiesBuilder(ALL).bicycleSafety(very_low_traffic).build()
    );
    props.setProperties(
      "highway=unclassified;maxspeed=70",
      new WayPropertiesBuilder(ALL).bicycleSafety(medium_high_traffic).build()
    );
    props.setProperties(
      "highway=unclassified;maxspeed=80",
      new WayPropertiesBuilder(ALL).bicycleSafety(medium_high_traffic).build()
    );

    props.setProperties(
      "highway=residential",
      new WayPropertiesBuilder(ALL).bicycleSafety(low_traffic).build()
    );
    // These access tags indicates low traffic
    props.setProperties(
      "highway=residential;motor_vehicle=no",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic).build()
    );
    props.setProperties(
      "highway=residential;motor_vehicle=private",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic).build()
    );
    props.setProperties(
      "highway=residential;motor_vehicle=permit",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic).build()
    );
    props.setProperties(
      "highway=residential;motor_vehicle=destination",
      new WayPropertiesBuilder(ALL).bicycleSafety(very_low_traffic).build()
    );

    props.setProperties(
      "highway=service",
      new WayPropertiesBuilder(ALL).bicycleSafety(low_traffic).build()
    );
    // Discourage cycling on roads with no infrastructure for neither walking nor cycling
    props.setProperties(
      "highway=service;foot=no",
      new WayPropertiesBuilder(BICYCLE_AND_CAR).bicycleSafety(medium_traffic).build()
    );
    // These access tags indicates low traffic
    props.setProperties(
      "highway=service;motor_vehicle=no",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic).build()
    );
    props.setProperties(
      "highway=service;motor_vehicle=private",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic).build()
    );
    props.setProperties(
      "highway=service;motor_vehicle=permit",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic).build()
    );
    props.setProperties(
      "highway=service;motor_vehicle=destination",
      new WayPropertiesBuilder(ALL).bicycleSafety(very_low_traffic).build()
    );
    // Cycling around reversing cars on a parking lot feels unsafe
    props.setProperties(
      "highway=service;service=parking_aisle",
      new WayPropertiesBuilder(ALL).bicycleSafety(medium_traffic).build()
    );
    props.setProperties(
      "highway=service;service=drive-through",
      new WayPropertiesBuilder(ALL).bicycleSafety(medium_traffic).build()
    );

    /* bicycle infrastructure */
    props.setProperties(
      "highway=trunk;cycleway=track;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(dual_lane_or_oneway_cycleway).build()
    );
    props.setProperties(
      "highway=trunk_link;cycleway=track;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(dual_lane_or_oneway_cycleway).build()
    );
    props.setProperties(
      "highway=primary;cycleway=track;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(dual_lane_or_oneway_cycleway).build()
    );
    props.setProperties(
      "highway=primary_link;cycleway=track;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(dual_lane_or_oneway_cycleway).build()
    );
    props.setProperties(
      "highway=secondary;cycleway=track;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(dual_lane_or_oneway_cycleway).build()
    );
    props.setProperties(
      "highway=secondary_link;cycleway=track;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(dual_lane_or_oneway_cycleway).build()
    );
    props.setProperties(
      "highway=tertiary;cycleway=track;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(dual_lane_or_oneway_cycleway).build()
    );
    props.setProperties(
      "highway=tertiary_link;cycleway=track;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(dual_lane_or_oneway_cycleway).build()
    );
    props.setProperties(
      "highway=unclassified;cycleway=track;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(dual_lane_or_oneway_cycleway).build()
    );
    props.setProperties(
      "highway=residential;cycleway=track;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(dual_lane_or_oneway_cycleway).build()
    );
    props.setProperties(
      "highway=living_street;cycleway=track;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(dual_lane_or_oneway_cycleway).build()
    );

    props.setProperties(
      "highway=trunk;cycleway=lane;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(cycle_lane_medium_traffic).build()
    );
    props.setProperties(
      "highway=trunk_link;cycleway=lane;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(cycle_lane_medium_traffic).build()
    );
    props.setProperties(
      "highway=primary;cycleway=lane;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(cycle_lane_medium_traffic).build()
    );
    props.setProperties(
      "highway=primary_link;cycleway=lane;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(cycle_lane_medium_traffic).build()
    );
    props.setProperties(
      "highway=secondary;cycleway=lane;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(cycle_lane_medium_traffic).build()
    );
    props.setProperties(
      "highway=secondary_link;cycleway=lane;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(cycle_lane_medium_traffic).build()
    );
    props.setProperties(
      "highway=secondary;cycleway=lane;maxspeed=40",
      new WayPropertiesBuilder(ALL).bicycleSafety(cycle_lane_low_traffic).build()
    );
    props.setProperties(
      "highway=secondary_link;cycleway=lane;maxspeed=40",
      new WayPropertiesBuilder(ALL).bicycleSafety(cycle_lane_low_traffic).build()
    );
    props.setProperties(
      "highway=secondary;cycleway=lane;maxspeed=30",
      new WayPropertiesBuilder(ALL).bicycleSafety(cycle_lane_low_traffic).build()
    );
    props.setProperties(
      "highway=secondary_link;cycleway=lane;maxspeed=30",
      new WayPropertiesBuilder(ALL).bicycleSafety(cycle_lane_low_traffic).build()
    );
    props.setProperties(
      "highway=tertiary;cycleway=lane;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(cycle_lane_medium_traffic).build()
    );
    props.setProperties(
      "highway=tertiary_link;cycleway=lane;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(cycle_lane_medium_traffic).build()
    );
    props.setProperties(
      "highway=tertiary;cycleway=lane;maxspeed=40",
      new WayPropertiesBuilder(ALL).bicycleSafety(cycle_lane_low_traffic).build()
    );
    props.setProperties(
      "highway=tertiary_link;cycleway=lane;maxspeed=40",
      new WayPropertiesBuilder(ALL).bicycleSafety(cycle_lane_low_traffic).build()
    );
    props.setProperties(
      "highway=tertiary;cycleway=lane;maxspeed=30",
      new WayPropertiesBuilder(ALL).bicycleSafety(cycle_lane_low_traffic).build()
    );
    props.setProperties(
      "highway=tertiary_link;cycleway=lane;maxspeed=30",
      new WayPropertiesBuilder(ALL).bicycleSafety(cycle_lane_low_traffic).build()
    );
    props.setProperties(
      "highway=unclassified;cycleway=lane;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(cycle_lane_medium_traffic).build()
    );
    props.setProperties(
      "highway=residential;cycleway=lane;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(cycle_lane_low_traffic).build()
    );
    props.setProperties(
      "highway=living_street;cycleway=lane;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(cycle_lane_low_traffic).build()
    );

    /* opposite */
    props.setProperties(
      "highway=trunk;cycleway=opposite_track;maxspeed=*",
      new WayPropertiesBuilder(ALL)
        .bicycleSafety(high_traffic, dual_lane_or_oneway_cycleway)
        .build()
    );
    props.setProperties(
      "highway=trunk_link;cycleway=opposite_track;maxspeed=*",
      new WayPropertiesBuilder(ALL)
        .bicycleSafety(high_traffic, dual_lane_or_oneway_cycleway)
        .build()
    );
    props.setProperties(
      "highway=primary;cycleway=opposite_track;maxspeed=*",
      new WayPropertiesBuilder(ALL)
        .bicycleSafety(high_traffic, dual_lane_or_oneway_cycleway)
        .build()
    );
    props.setProperties(
      "highway=primary_link;cycleway=opposite_track;maxspeed=*",
      new WayPropertiesBuilder(ALL)
        .bicycleSafety(high_traffic, dual_lane_or_oneway_cycleway)
        .build()
    );
    props.setProperties(
      "highway=secondary;cycleway=opposite_track;maxspeed=*",
      new WayPropertiesBuilder(ALL)
        .bicycleSafety(medium_high_traffic, dual_lane_or_oneway_cycleway)
        .build()
    );
    props.setProperties(
      "highway=secondary_link;cycleway=opposite_track;maxspeed=*",
      new WayPropertiesBuilder(ALL)
        .bicycleSafety(medium_high_traffic, dual_lane_or_oneway_cycleway)
        .build()
    );
    props.setProperties(
      "highway=tertiary;cycleway=opposite_track;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(low_traffic, dual_lane_or_oneway_cycleway).build()
    );
    props.setProperties(
      "highway=tertiary_link;cycleway=opposite_track;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(low_traffic, dual_lane_or_oneway_cycleway).build()
    );
    props.setProperties(
      "highway=unclassified;cycleway=opposite_track;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(low_traffic, dual_lane_or_oneway_cycleway).build()
    );
    props.setProperties(
      "highway=residential;cycleway=opposite_track;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(low_traffic, dual_lane_or_oneway_cycleway).build()
    );
    props.setProperties(
      "highway=living_street;cycleway=opposite_track;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(low_traffic, dual_lane_or_oneway_cycleway).build()
    );

    props.setProperties(
      "highway=trunk;cycleway=opposite_lane;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(high_traffic, cycle_lane_medium_traffic).build()
    );
    props.setProperties(
      "highway=trunk_link;cycleway=opposite_lane;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(high_traffic, cycle_lane_medium_traffic).build()
    );
    props.setProperties(
      "highway=primary;cycleway=opposite_lane;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(high_traffic, cycle_lane_medium_traffic).build()
    );
    props.setProperties(
      "highway=primary_link;cycleway=opposite_lane;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(high_traffic, cycle_lane_medium_traffic).build()
    );
    props.setProperties(
      "highway=secondary;cycleway=opposite_lane;maxspeed=*",
      new WayPropertiesBuilder(ALL)
        .bicycleSafety(medium_high_traffic, cycle_lane_medium_traffic)
        .build()
    );
    props.setProperties(
      "highway=secondary_link;cycleway=opposite_lane;maxspeed=*",
      new WayPropertiesBuilder(ALL)
        .bicycleSafety(medium_high_traffic, cycle_lane_medium_traffic)
        .build()
    );
    props.setProperties(
      "highway=tertiary;cycleway=opposite_lane;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(low_traffic, cycle_lane_low_traffic).build()
    );
    props.setProperties(
      "highway=tertiary_link;cycleway=opposite_lane;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(low_traffic, cycle_lane_low_traffic).build()
    );
    props.setProperties(
      "highway=unclassified;cycleway=opposite_lane;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(low_traffic, cycle_lane_low_traffic).build()
    );
    props.setProperties(
      "highway=residential;cycleway=opposite_lane;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(low_traffic, cycle_lane_low_traffic).build()
    );
    props.setProperties(
      "highway=living_street;cycleway=opposite_lane;maxspeed=*",
      new WayPropertiesBuilder(ALL).bicycleSafety(low_traffic, cycle_lane_low_traffic).build()
    );

    /* Pedestrian, living and cyclestreet */
    props.setProperties(
      "highway=living_street",
      new WayPropertiesBuilder(ALL).bicycleSafety(low_traffic).build()
    );
    props.setProperties(
      "highway=pedestrian",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.2).build()
    );
    props.setProperties(
      "highway=residential;cyclestreet=yes;motor_vehicle=*",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic).build()
    );

    props.setProperties(
      "highway=footway",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(dedicated_footway).build()
    );
    // "motor_vehicle=destination" indicates unwanted car traffic, signposted "Kjøring til eiendommene tillatt"
    props.setProperties(
      "highway=footway;motor_vehicle=destination",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic).build()
    );

    props.setProperties(
      "highway=footway;footway=sidewalk",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(sidewalk).build()
    );
    props.setProperties(
      "highway=footway;footway=crossing",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(footway_crossing).build()
    );
    props.setProperties(
      "highway=cycleway;footway=sidewalk",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(dedicated_footway).build()
    );
    props.setProperties(
      "highway=cycleway;footway=crossing",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(footway_crossing).build()
    );
    props.setProperties(
      "highway=cycleway;cycleway=crossing",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(footway_crossing).build()
    );

    props.setProperties(
      "highway=cycleway",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(dedicated_cycleway).build()
    );
    props.setProperties(
      "highway=cycleway;lanes=2",
      new WayPropertiesBuilder(BICYCLE).bicycleSafety(dual_lane_or_oneway_cycleway).build()
    );
    props.setProperties(
      "highway=cycleway;oneway=yes",
      new WayPropertiesBuilder(BICYCLE).bicycleSafety(dual_lane_or_oneway_cycleway).build()
    );
    // "motor_vehicle=destination" indicates unwanted car traffic, signposted "Kjøring til eiendommene tillatt"
    props.setProperties(
      "highway=cycleway;motor_vehicle=destination",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic).build()
    );

    // segregated=no takes' precedence if there is no "segregated" key. There is no penalty for a tag mismatch
    props.setProperties(
      "highway=cycleway;foot=designated;segregated=no",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(mixed_cycleway).build()
    );
    props.setProperties(
      "highway=cycleway;foot=designated;segregated=yes",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(dedicated_cycleway).build()
    );
    props.setProperties(
      "highway=cycleway;foot=designated;segregated=yes;lanes=2",
      new WayPropertiesBuilder(BICYCLE).bicycleSafety(dual_lane_or_oneway_cycleway).build()
    );
    props.setProperties(
      "highway=path;foot=designated;bicycle=designated;segregated=no",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(mixed_cycleway).build()
    );
    props.setProperties(
      "highway=path;foot=designated;bicycle=designated;segregated=yes",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(dedicated_cycleway).build()
    );
    // "motor_vehicle=destination" indicates unwanted car traffic, signposted "Kjøring til eiendommene tillatt"
    props.setProperties(
      "highway=cycleway;foot=designated;segregated=*;motor_vehicle=destination",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic).build()
    );
    props.setProperties(
      "highway=path;foot=designated;bicycle=designated;segregated=*;motor_vehicle=destination",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic).build()
    );

    //relation properties are copied over to ways
    props.setMixinProperties(
      "lcn=yes|rcn=yes|ncn=yes",
      new WayPropertiesBuilder(ALL).bicycleSafety(0.8).build()
    );

    props.setProperties(
      "highway=busway",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(low_traffic).build()
    );
    props.setProperties(
      "highway=track",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.1).build()
    );
    props.setProperties(
      "highway=bridleway",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.1).build()
    );
    props.setProperties(
      "highway=path",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.5).build()
    );
    props.setProperties("highway=steps", new WayPropertiesBuilder(PEDESTRIAN).build());
    props.setProperties("highway=corridor", new WayPropertiesBuilder(PEDESTRIAN).build());
    props.setProperties("highway=footway;indoor=yes", new WayPropertiesBuilder(PEDESTRIAN).build());
    props.setProperties("highway=platform", new WayPropertiesBuilder(PEDESTRIAN).build());
    props.setProperties("public_transport=platform", new WayPropertiesBuilder(PEDESTRIAN).build());

    props.setMixinProperties(
      "smoothness=intermediate",
      new WayPropertiesBuilder(ALL).bicycleSafety(1.5).build()
    );
    props.setMixinProperties(
      "smoothness=bad",
      new WayPropertiesBuilder(ALL).bicycleSafety(2).build()
    );
    props.setProperties(
      "highway=*;smoothness=very_bad",
      new WayPropertiesBuilder(PEDESTRIAN).build()
    );
    props.setProperties("highway=*;smoothness=horrible", new WayPropertiesBuilder(NONE).build());
    props.setProperties(
      "highway=*;smoothness=very_horrible",
      new WayPropertiesBuilder(NONE).build()
    );
    props.setProperties("highway=*;smoothness=impassable", new WayPropertiesBuilder(NONE).build());

    props.setProperties("highway=*;mtb:scale=1", new WayPropertiesBuilder(PEDESTRIAN).build());
    props.setProperties("highway=*;mtb:scale=2", new WayPropertiesBuilder(PEDESTRIAN).build());
    props.setProperties("highway=*;mtb:scale=3", new WayPropertiesBuilder(NONE).build());
    props.setProperties("highway=*;mtb:scale=4", new WayPropertiesBuilder(NONE).build());
    props.setProperties("highway=*;mtb:scale=5", new WayPropertiesBuilder(NONE).build());
    props.setProperties("highway=*;mtb:scale=6", new WayPropertiesBuilder(NONE).build());

    props.setProperties(
      "highway=track;tracktype=grade1",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic).build()
    );
    props.setProperties(
      "highway=track;tracktype=grade2",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.1).build()
    );
    props.setProperties(
      "highway=track;tracktype=grade3",
      new WayPropertiesBuilder(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.5).build()
    );
    props.setProperties(
      "highway=track;tracktype=grade4",
      new WayPropertiesBuilder(PEDESTRIAN).build()
    );
    props.setProperties(
      "highway=track;tracktype=grade5",
      new WayPropertiesBuilder(PEDESTRIAN).build()
    );

    props.setProperties(
      "highway=path;trail_visibility=bad",
      new WayPropertiesBuilder(NONE).build()
    );
    props.setProperties("highway=path;trail_visibility=no", new WayPropertiesBuilder(NONE).build());
    props.setProperties(
      "highway=path;trail_visibility=low",
      new WayPropertiesBuilder(NONE).build()
    );
    props.setProperties(
      "highway=path;trail_visibility=poor",
      new WayPropertiesBuilder(NONE).build()
    );

    props.setProperties(
      "highway=path;sac_scale=mountain_hiking",
      new WayPropertiesBuilder(NONE).build()
    );
    props.setProperties(
      "highway=path;sac_scale=demanding_mountain_hiking",
      new WayPropertiesBuilder(NONE).build()
    );
    props.setProperties(
      "highway=path;sac_scale=alpine_hiking",
      new WayPropertiesBuilder(NONE).build()
    );
    props.setProperties(
      "highway=path;sac_scale=demanding_alpine_hiking",
      new WayPropertiesBuilder(NONE).build()
    );
    props.setProperties(
      "highway=path;sac_scale=difficult_alpine_hiking",
      new WayPropertiesBuilder(NONE).build()
    );

    // paved but unfavorable
    props.setMixinProperties(
      "surface=grass_paver",
      new WayPropertiesBuilder(ALL).bicycleSafety(1.2).build()
    );
    props.setMixinProperties(
      "surface=sett",
      new WayPropertiesBuilder(ALL).bicycleSafety(1.2).build()
    );
    props.setMixinProperties(
      "surface=cobblestone",
      new WayPropertiesBuilder(ALL).bicycleSafety(1.2).build()
    );
    props.setMixinProperties(
      "surface=unhewn_cobblestone",
      new WayPropertiesBuilder(ALL).bicycleSafety(1.5).build()
    );
    // Can be slick if wet, but otherwise not unfavorable to bikes
    props.setMixinProperties(
      "surface=metal_grid",
      new WayPropertiesBuilder(ALL).bicycleSafety(1.2).build()
    );
    props.setMixinProperties(
      "surface=metal",
      new WayPropertiesBuilder(ALL).bicycleSafety(1.2).build()
    );

    // unpaved
    props.setMixinProperties(
      "surface=unpaved",
      new WayPropertiesBuilder(ALL).bicycleSafety(1.2).build()
    );
    props.setMixinProperties(
      "surface=compacted",
      new WayPropertiesBuilder(ALL).bicycleSafety(1.2).build()
    );
    props.setMixinProperties(
      "surface=fine_gravel",
      new WayPropertiesBuilder(ALL).bicycleSafety(1.3).build()
    );
    props.setMixinProperties(
      "surface=pebblestone",
      new WayPropertiesBuilder(ALL).bicycleSafety(1.3).build()
    );
    props.setMixinProperties(
      "surface=gravel",
      new WayPropertiesBuilder(ALL).bicycleSafety(1.3).build()
    );
    props.setMixinProperties(
      "surface=woodchip",
      new WayPropertiesBuilder(ALL).bicycleSafety(1.5).build()
    );
    props.setMixinProperties(
      "surface=ground",
      new WayPropertiesBuilder(ALL).bicycleSafety(1.5).build()
    );
    props.setMixinProperties(
      "surface=dirt",
      new WayPropertiesBuilder(ALL).bicycleSafety(1.5).build()
    );
    props.setMixinProperties(
      "surface=earth",
      new WayPropertiesBuilder(ALL).bicycleSafety(1.5).build()
    );
    props.setMixinProperties(
      "surface=grass",
      new WayPropertiesBuilder(ALL).bicycleSafety(1.5).build()
    );
    props.setMixinProperties("surface=mud", new WayPropertiesBuilder(ALL).bicycleSafety(2).build());
    props.setMixinProperties(
      "surface=sand",
      new WayPropertiesBuilder(ALL).bicycleSafety(2).build()
    );

    /*
     * Automobile speeds in Norway. General speed limit is 80kph unless signs says otherwise
     *
     */

    props.setCarSpeed("highway=motorway", 22.22f); // 80 km/t
    props.setCarSpeed("highway=motorway_link", 22.22f); // 80 km/t
    props.setCarSpeed("highway=trunk", 22.22f); // 80 km/t
    props.setCarSpeed("highway=trunk_link", 22.22f); // 80 km/t
    props.setCarSpeed("highway=primary", 22.22f); // 80 km/t
    props.setCarSpeed("highway=primary_link", 22.22f); // 80 km/t
    props.setCarSpeed("highway=secondary", 22.22f); // 80 km/t
    props.setCarSpeed("highway=secondary_link", 22.22f); // 80 km/t
    props.setCarSpeed("highway=tertiary", 22.22f); // 80 km/t
    props.setCarSpeed("highway=tertiary_link", 22.22f); // 80 km/t
    props.setCarSpeed("highway=living_street", 1.94f); // 7 km/t

    props.setCarSpeed("highway=pedestrian", 1.94f); // 7 km/t

    props.setCarSpeed("highway=residential", 8.33f); // 30 km/t
    props.setCarSpeed("highway=unclassified", 22.22f); // 80 km/t
    props.setCarSpeed("highway=service", 13.89f); // 50 km/t
    props.setCarSpeed("highway=track", 8.33f); // 30 km/t
    props.setCarSpeed("highway=road", 13.89f); // 50 km/t

    props.defaultSpeed = 22.22f; // 80kph

    new DefaultWayPropertySetSource().populateNotesAndNames(props);

    props.setSlopeOverride(new OSMSpecifier("bridge=*"), true);
    props.setSlopeOverride(new OSMSpecifier("embankment=*"), false);
    props.setSlopeOverride(new OSMSpecifier("tunnel=*"), true);
    props.setSlopeOverride(new OSMSpecifier("location=underground"), true);
    props.setSlopeOverride(new OSMSpecifier("indoor=yes"), true);
  }

  @Override
  public DrivingDirection drivingDirection() {
    return drivingDirection;
  }

  @Override
  public IntersectionTraversalCostModel getIntersectionTraversalCostModel() {
    return new NorwayIntersectionTraversalCostModel(drivingDirection);
  }
}
