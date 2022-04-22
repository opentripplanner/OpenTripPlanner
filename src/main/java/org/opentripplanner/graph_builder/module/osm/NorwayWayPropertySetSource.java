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

    props.setProperties("highway=motorway", CAR);
    props.setProperties("highway=motorway_link", CAR);

    // Do not walk on "Motortrafikkvei" ("motorvei klasse b")
    props.setProperties("highway=trunk;motorroad=yes", CAR);
    props.setProperties("highway=trunk_link;motorroad=yes", CAR);

    props.setProperties("highway=primary;motorroad=yes", CAR);
    props.setProperties("highway=primary_link;motorroad=yes", CAR);

    /* "highway=trunk" roads ("Riksveier") are often single carriageway, and not only legal for bicycles,
    but assumed to be  much safer than the  default profile (Except when the speedlimt exceeds 90). */
    props.setProperties("highway=trunk", ALL, high_traffic, high_traffic);
    props.setProperties("highway=trunk_link", ALL, high_traffic, high_traffic);
    // Discourage cycling on roads with no infrastructure for neither walking nor cycling
    props.setProperties(
      "highway=trunk;foot=no",
      BICYCLE_AND_CAR,
      very_high_traffic,
      very_high_traffic
    );
    props.setProperties(
      "highway=trunk_link;foot=no",
      BICYCLE_AND_CAR,
      very_high_traffic,
      very_high_traffic
    );
    // Discourage cycling on trunk road tunnels
    props.setProperties("highway=trunk;tunnel=yes", CAR, very_high_traffic, very_high_traffic);
    props.setProperties("highway=trunk_link;tunnel=yes", CAR, very_high_traffic, very_high_traffic);
    props.setProperties("highway=trunk;maxspeed=90", ALL, very_high_traffic, very_high_traffic);
    props.setProperties(
      "highway=trunk_link;maxspeed=90",
      ALL,
      very_high_traffic,
      very_high_traffic
    );
    props.setProperties("highway=trunk;maxspeed=60", ALL, medium_high_traffic, medium_high_traffic);
    props.setProperties(
      "highway=trunk_link;maxspeed=60",
      ALL,
      medium_high_traffic,
      medium_high_traffic
    );
    props.setProperties("highway=trunk;maxspeed=50", ALL, medium_traffic, medium_traffic);
    props.setProperties("highway=trunk_link;maxspeed=50", ALL, medium_traffic, medium_traffic);
    props.setProperties("highway=trunk;maxspeed=40", ALL, medium_traffic, medium_traffic);
    props.setProperties("highway=trunk_link;maxspeed=40", ALL, medium_traffic, medium_traffic);

    props.setProperties("highway=primary", ALL, high_traffic, high_traffic);
    props.setProperties("highway=primary_link", ALL, high_traffic, high_traffic);
    // Discourage cycling on roads with no infrastructure for neither walking nor cycling
    props.setProperties(
      "highway=primary;foot=no",
      BICYCLE_AND_CAR,
      very_high_traffic,
      very_high_traffic
    );
    props.setProperties(
      "highway=primary_link;foot=no",
      BICYCLE_AND_CAR,
      very_high_traffic,
      very_high_traffic
    );
    props.setProperties("highway=primary;maxspeed=90", ALL, very_high_traffic, very_high_traffic);
    props.setProperties(
      "highway=primary_link;maxspeed=90",
      ALL,
      very_high_traffic,
      very_high_traffic
    );
    props.setProperties(
      "highway=primary;maxspeed=60",
      ALL,
      medium_high_traffic,
      medium_high_traffic
    );
    props.setProperties(
      "highway=primary_link;maxspeed=60",
      ALL,
      medium_high_traffic,
      medium_high_traffic
    );
    props.setProperties("highway=primary;maxspeed=50", ALL, medium_traffic, medium_traffic);
    props.setProperties("highway=primary_link;maxspeed=50", ALL, medium_traffic, medium_traffic);
    props.setProperties("highway=primary;maxspeed=40", ALL, medium_traffic, medium_traffic);
    props.setProperties("highway=primary_link;maxspeed=40", ALL, medium_traffic, medium_traffic);
    props.setProperties("highway=primary;maxspeed=30", ALL, low_traffic, low_traffic);
    props.setProperties("highway=primary_link;maxspeed=30", ALL, low_traffic, low_traffic);

    props.setProperties("highway=secondary", ALL, medium_high_traffic, medium_high_traffic);
    props.setProperties("highway=secondary_link", ALL, medium_high_traffic, medium_high_traffic);
    // Discourage cycling on roads with no infrastructure for neither walking nor cycling
    props.setProperties(
      "highway=secondary;foot=no",
      BICYCLE_AND_CAR,
      very_high_traffic,
      very_high_traffic
    );
    props.setProperties(
      "highway=secondary_link;foot=no",
      BICYCLE_AND_CAR,
      very_high_traffic,
      very_high_traffic
    );
    props.setProperties("highway=secondary;maxspeed=60", ALL, medium_traffic, medium_traffic);
    props.setProperties("highway=secondary_link;maxspeed=60", ALL, medium_traffic, medium_traffic);
    props.setProperties(
      "highway=secondary;maxspeed=50",
      ALL,
      medium_low_traffic,
      medium_low_traffic
    );
    props.setProperties(
      "highway=secondary_link;maxspeed=50",
      ALL,
      medium_low_traffic,
      medium_low_traffic
    );
    props.setProperties(
      "highway=secondary;maxspeed=40",
      ALL,
      medium_low_traffic,
      medium_low_traffic
    );
    props.setProperties(
      "highway=secondary_link;maxspeed=40",
      ALL,
      medium_low_traffic,
      medium_low_traffic
    );
    props.setProperties("highway=secondary;maxspeed=30", ALL, low_traffic, low_traffic);
    props.setProperties("highway=secondary_link;maxspeed=30", ALL, low_traffic, low_traffic);

    props.setProperties("highway=tertiary", ALL, medium_low_traffic, medium_low_traffic);
    props.setProperties("highway=tertiary_link", ALL, medium_low_traffic, medium_low_traffic);
    // Discourage cycling on roads with no infrastructure for neither walking nor cycling
    props.setProperties(
      "highway=tertiary;foot=no",
      BICYCLE_AND_CAR,
      very_high_traffic,
      very_high_traffic
    );
    props.setProperties(
      "highway=tertiary_link;foot=no",
      BICYCLE_AND_CAR,
      very_high_traffic,
      very_high_traffic
    );
    props.setProperties(
      "highway=tertiary;maxspeed=80",
      ALL,
      medium_high_traffic,
      medium_high_traffic
    );
    props.setProperties(
      "highway=tertiary_link;maxspeed=80",
      ALL,
      medium_high_traffic,
      medium_high_traffic
    );
    props.setProperties(
      "highway=tertiary;maxspeed=70",
      ALL,
      medium_high_traffic,
      medium_high_traffic
    );
    props.setProperties(
      "highway=tertiary_link;maxspeed=70",
      ALL,
      medium_high_traffic,
      medium_high_traffic
    );
    props.setProperties("highway=tertiary;maxspeed=40", ALL, low_traffic, low_traffic);
    props.setProperties("highway=tertiary_link;maxspeed=40", ALL, low_traffic, low_traffic);
    props.setProperties("highway=tertiary;maxspeed=30", ALL, low_traffic, low_traffic);
    props.setProperties("highway=tertiary_link;maxspeed=30", ALL, low_traffic, low_traffic);

    props.setProperties("highway=unclassified", ALL, low_traffic, low_traffic);
    // Discourage cycling on roads with no infrastructure for neither walking nor cycling
    props.setProperties(
      "highway=unclassified;foot=no",
      BICYCLE_AND_CAR,
      medium_traffic,
      medium_traffic
    );
    // These access tags indicates low traffic
    props.setProperties(
      "highway=unclassified;motor_vehicle=no",
      PEDESTRIAN_AND_BICYCLE,
      very_low_traffic,
      very_low_traffic
    );
    props.setProperties(
      "highway=unclassified;motor_vehicle=private",
      PEDESTRIAN_AND_BICYCLE,
      very_low_traffic,
      very_low_traffic
    );
    props.setProperties(
      "highway=unclassified;motor_vehicle=permit",
      PEDESTRIAN_AND_BICYCLE,
      very_low_traffic,
      very_low_traffic
    );
    props.setProperties(
      "highway=unclassified;motor_vehicle=destination",
      ALL,
      very_low_traffic,
      very_low_traffic
    );
    props.setProperties(
      "highway=unclassified;maxspeed=70",
      ALL,
      medium_high_traffic,
      medium_high_traffic
    );
    props.setProperties(
      "highway=unclassified;maxspeed=80",
      ALL,
      medium_high_traffic,
      medium_high_traffic
    );

    props.setProperties("highway=residential", ALL, low_traffic, low_traffic);
    // These access tags indicates low traffic
    props.setProperties(
      "highway=residential;motor_vehicle=no",
      PEDESTRIAN_AND_BICYCLE,
      very_low_traffic,
      very_low_traffic
    );
    props.setProperties(
      "highway=residential;motor_vehicle=private",
      PEDESTRIAN_AND_BICYCLE,
      very_low_traffic,
      very_low_traffic
    );
    props.setProperties(
      "highway=residential;motor_vehicle=permit",
      PEDESTRIAN_AND_BICYCLE,
      very_low_traffic,
      very_low_traffic
    );
    props.setProperties(
      "highway=residential;motor_vehicle=destination",
      ALL,
      very_low_traffic,
      very_low_traffic
    );

    props.setProperties("highway=service", ALL, low_traffic, low_traffic);
    // Discourage cycling on roads with no infrastructure for neither walking nor cycling
    props.setProperties("highway=service;foot=no", BICYCLE_AND_CAR, medium_traffic, medium_traffic);
    // These access tags indicates low traffic
    props.setProperties(
      "highway=service;motor_vehicle=no",
      PEDESTRIAN_AND_BICYCLE,
      very_low_traffic,
      very_low_traffic
    );
    props.setProperties(
      "highway=service;motor_vehicle=private",
      PEDESTRIAN_AND_BICYCLE,
      very_low_traffic,
      very_low_traffic
    );
    props.setProperties(
      "highway=service;motor_vehicle=permit",
      PEDESTRIAN_AND_BICYCLE,
      very_low_traffic,
      very_low_traffic
    );
    props.setProperties(
      "highway=service;motor_vehicle=destination",
      ALL,
      very_low_traffic,
      very_low_traffic
    );
    // Cycling around reversing cars on a parking lot feels unsafe
    props.setProperties(
      "highway=service;service=parking_aisle",
      ALL,
      medium_traffic,
      medium_traffic
    );
    props.setProperties(
      "highway=service;service=drive-through",
      ALL,
      medium_traffic,
      medium_traffic
    );

    /* bicycle infrastructure */
    props.setProperties(
      "highway=trunk;cycleway=track;maxspeed=*",
      ALL,
      dual_lane_or_oneway_cycleway,
      dual_lane_or_oneway_cycleway
    );
    props.setProperties(
      "highway=trunk_link;cycleway=track;maxspeed=*",
      ALL,
      dual_lane_or_oneway_cycleway,
      dual_lane_or_oneway_cycleway
    );
    props.setProperties(
      "highway=primary;cycleway=track;maxspeed=*",
      ALL,
      dual_lane_or_oneway_cycleway,
      dual_lane_or_oneway_cycleway
    );
    props.setProperties(
      "highway=primary_link;cycleway=track;maxspeed=*",
      ALL,
      dual_lane_or_oneway_cycleway,
      dual_lane_or_oneway_cycleway
    );
    props.setProperties(
      "highway=secondary;cycleway=track;maxspeed=*",
      ALL,
      dual_lane_or_oneway_cycleway,
      dual_lane_or_oneway_cycleway
    );
    props.setProperties(
      "highway=secondary_link;cycleway=track;maxspeed=*",
      ALL,
      dual_lane_or_oneway_cycleway,
      dual_lane_or_oneway_cycleway
    );
    props.setProperties(
      "highway=tertiary;cycleway=track;maxspeed=*",
      ALL,
      dual_lane_or_oneway_cycleway,
      dual_lane_or_oneway_cycleway
    );
    props.setProperties(
      "highway=tertiary_link;cycleway=track;maxspeed=*",
      ALL,
      dual_lane_or_oneway_cycleway,
      dual_lane_or_oneway_cycleway
    );
    props.setProperties(
      "highway=unclassified;cycleway=track;maxspeed=*",
      ALL,
      dual_lane_or_oneway_cycleway,
      dual_lane_or_oneway_cycleway
    );
    props.setProperties(
      "highway=residential;cycleway=track;maxspeed=*",
      ALL,
      dual_lane_or_oneway_cycleway,
      dual_lane_or_oneway_cycleway
    );
    props.setProperties(
      "highway=living_street;cycleway=track;maxspeed=*",
      ALL,
      dual_lane_or_oneway_cycleway,
      dual_lane_or_oneway_cycleway
    );

    props.setProperties(
      "highway=trunk;cycleway=lane;maxspeed=*",
      ALL,
      cycle_lane_medium_traffic,
      cycle_lane_medium_traffic
    );
    props.setProperties(
      "highway=trunk_link;cycleway=lane;maxspeed=*",
      ALL,
      cycle_lane_medium_traffic,
      cycle_lane_medium_traffic
    );
    props.setProperties(
      "highway=primary;cycleway=lane;maxspeed=*",
      ALL,
      cycle_lane_medium_traffic,
      cycle_lane_medium_traffic
    );
    props.setProperties(
      "highway=primary_link;cycleway=lane;maxspeed=*",
      ALL,
      cycle_lane_medium_traffic,
      cycle_lane_medium_traffic
    );
    props.setProperties(
      "highway=secondary;cycleway=lane;maxspeed=*",
      ALL,
      cycle_lane_medium_traffic,
      cycle_lane_medium_traffic
    );
    props.setProperties(
      "highway=secondary_link;cycleway=lane;maxspeed=*",
      ALL,
      cycle_lane_medium_traffic,
      cycle_lane_medium_traffic
    );
    props.setProperties(
      "highway=secondary;cycleway=lane;maxspeed=40",
      ALL,
      cycle_lane_low_traffic,
      cycle_lane_low_traffic
    );
    props.setProperties(
      "highway=secondary_link;cycleway=lane;maxspeed=40",
      ALL,
      cycle_lane_low_traffic,
      cycle_lane_low_traffic
    );
    props.setProperties(
      "highway=secondary;cycleway=lane;maxspeed=30",
      ALL,
      cycle_lane_low_traffic,
      cycle_lane_low_traffic
    );
    props.setProperties(
      "highway=secondary_link;cycleway=lane;maxspeed=30",
      ALL,
      cycle_lane_low_traffic,
      cycle_lane_low_traffic
    );
    props.setProperties(
      "highway=tertiary;cycleway=lane;maxspeed=*",
      ALL,
      cycle_lane_medium_traffic,
      cycle_lane_medium_traffic
    );
    props.setProperties(
      "highway=tertiary_link;cycleway=lane;maxspeed=*",
      ALL,
      cycle_lane_medium_traffic,
      cycle_lane_medium_traffic
    );
    props.setProperties(
      "highway=tertiary;cycleway=lane;maxspeed=40",
      ALL,
      cycle_lane_low_traffic,
      cycle_lane_low_traffic
    );
    props.setProperties(
      "highway=tertiary_link;cycleway=lane;maxspeed=40",
      ALL,
      cycle_lane_low_traffic,
      cycle_lane_low_traffic
    );
    props.setProperties(
      "highway=tertiary;cycleway=lane;maxspeed=30",
      ALL,
      cycle_lane_low_traffic,
      cycle_lane_low_traffic
    );
    props.setProperties(
      "highway=tertiary_link;cycleway=lane;maxspeed=30",
      ALL,
      cycle_lane_low_traffic,
      cycle_lane_low_traffic
    );
    props.setProperties(
      "highway=unclassified;cycleway=lane;maxspeed=*",
      ALL,
      cycle_lane_medium_traffic,
      cycle_lane_medium_traffic
    );
    props.setProperties(
      "highway=residential;cycleway=lane;maxspeed=*",
      ALL,
      cycle_lane_low_traffic,
      cycle_lane_low_traffic
    );
    props.setProperties(
      "highway=living_street;cycleway=lane;maxspeed=*",
      ALL,
      cycle_lane_low_traffic,
      cycle_lane_low_traffic
    );

    /* opposite */
    props.setProperties(
      "highway=trunk;cycleway=opposite_track;maxspeed=*",
      ALL,
      high_traffic,
      dual_lane_or_oneway_cycleway
    );
    props.setProperties(
      "highway=trunk_link;cycleway=opposite_track;maxspeed=*",
      ALL,
      high_traffic,
      dual_lane_or_oneway_cycleway
    );
    props.setProperties(
      "highway=primary;cycleway=opposite_track;maxspeed=*",
      ALL,
      high_traffic,
      dual_lane_or_oneway_cycleway
    );
    props.setProperties(
      "highway=primary_link;cycleway=opposite_track;maxspeed=*",
      ALL,
      high_traffic,
      dual_lane_or_oneway_cycleway
    );
    props.setProperties(
      "highway=secondary;cycleway=opposite_track;maxspeed=*",
      ALL,
      medium_high_traffic,
      dual_lane_or_oneway_cycleway
    );
    props.setProperties(
      "highway=secondary_link;cycleway=opposite_track;maxspeed=*",
      ALL,
      medium_high_traffic,
      dual_lane_or_oneway_cycleway
    );
    props.setProperties(
      "highway=tertiary;cycleway=opposite_track;maxspeed=*",
      ALL,
      low_traffic,
      dual_lane_or_oneway_cycleway
    );
    props.setProperties(
      "highway=tertiary_link;cycleway=opposite_track;maxspeed=*",
      ALL,
      low_traffic,
      dual_lane_or_oneway_cycleway
    );
    props.setProperties(
      "highway=unclassified;cycleway=opposite_track;maxspeed=*",
      ALL,
      low_traffic,
      dual_lane_or_oneway_cycleway
    );
    props.setProperties(
      "highway=residential;cycleway=opposite_track;maxspeed=*",
      ALL,
      low_traffic,
      dual_lane_or_oneway_cycleway
    );
    props.setProperties(
      "highway=living_street;cycleway=opposite_track;maxspeed=*",
      ALL,
      low_traffic,
      dual_lane_or_oneway_cycleway
    );

    props.setProperties(
      "highway=trunk;cycleway=opposite_lane;maxspeed=*",
      ALL,
      high_traffic,
      cycle_lane_medium_traffic
    );
    props.setProperties(
      "highway=trunk_link;cycleway=opposite_lane;maxspeed=*",
      ALL,
      high_traffic,
      cycle_lane_medium_traffic
    );
    props.setProperties(
      "highway=primary;cycleway=opposite_lane;maxspeed=*",
      ALL,
      high_traffic,
      cycle_lane_medium_traffic
    );
    props.setProperties(
      "highway=primary_link;cycleway=opposite_lane;maxspeed=*",
      ALL,
      high_traffic,
      cycle_lane_medium_traffic
    );
    props.setProperties(
      "highway=secondary;cycleway=opposite_lane;maxspeed=*",
      ALL,
      medium_high_traffic,
      cycle_lane_medium_traffic
    );
    props.setProperties(
      "highway=secondary_link;cycleway=opposite_lane;maxspeed=*",
      ALL,
      medium_high_traffic,
      cycle_lane_medium_traffic
    );
    props.setProperties(
      "highway=tertiary;cycleway=opposite_lane;maxspeed=*",
      ALL,
      low_traffic,
      cycle_lane_low_traffic
    );
    props.setProperties(
      "highway=tertiary_link;cycleway=opposite_lane;maxspeed=*",
      ALL,
      low_traffic,
      cycle_lane_low_traffic
    );
    props.setProperties(
      "highway=unclassified;cycleway=opposite_lane;maxspeed=*",
      ALL,
      low_traffic,
      cycle_lane_low_traffic
    );
    props.setProperties(
      "highway=residential;cycleway=opposite_lane;maxspeed=*",
      ALL,
      low_traffic,
      cycle_lane_low_traffic
    );
    props.setProperties(
      "highway=living_street;cycleway=opposite_lane;maxspeed=*",
      ALL,
      low_traffic,
      cycle_lane_low_traffic
    );

    /* Pedestrian, living and cyclestreet */
    props.setProperties("highway=living_street", ALL, low_traffic, low_traffic);
    props.setProperties("highway=pedestrian", PEDESTRIAN_AND_BICYCLE, 1.2, 1.2);
    props.setProperties(
      "highway=residential;cyclestreet=yes;motor_vehicle=*",
      PEDESTRIAN_AND_BICYCLE,
      very_low_traffic,
      very_low_traffic
    );

    props.setProperties(
      "highway=footway",
      PEDESTRIAN_AND_BICYCLE,
      dedicated_footway,
      dedicated_footway
    );
    // "motor_vehicle=destination" indicates unwanted car traffic, signposted "Kjøring til eiendommene tillatt"
    props.setProperties(
      "highway=footway;motor_vehicle=destination",
      PEDESTRIAN_AND_BICYCLE,
      very_low_traffic,
      very_low_traffic
    );

    props.setProperties(
      "highway=footway;footway=sidewalk",
      PEDESTRIAN_AND_BICYCLE,
      sidewalk,
      sidewalk
    );
    props.setProperties(
      "highway=footway;footway=crossing",
      PEDESTRIAN_AND_BICYCLE,
      footway_crossing,
      footway_crossing
    );
    props.setProperties(
      "highway=cycleway;footway=sidewalk",
      PEDESTRIAN_AND_BICYCLE,
      dedicated_footway,
      dedicated_footway
    );
    props.setProperties(
      "highway=cycleway;footway=crossing",
      PEDESTRIAN_AND_BICYCLE,
      footway_crossing,
      footway_crossing
    );
    props.setProperties(
      "highway=cycleway;cycleway=crossing",
      PEDESTRIAN_AND_BICYCLE,
      footway_crossing,
      footway_crossing
    );

    props.setProperties(
      "highway=cycleway",
      PEDESTRIAN_AND_BICYCLE,
      dedicated_cycleway,
      dedicated_cycleway
    );
    props.setProperties(
      "highway=cycleway;lanes=2",
      BICYCLE,
      dual_lane_or_oneway_cycleway,
      dual_lane_or_oneway_cycleway
    );
    props.setProperties(
      "highway=cycleway;oneway=yes",
      BICYCLE,
      dual_lane_or_oneway_cycleway,
      dual_lane_or_oneway_cycleway
    );
    // "motor_vehicle=destination" indicates unwanted car traffic, signposted "Kjøring til eiendommene tillatt"
    props.setProperties(
      "highway=cycleway;motor_vehicle=destination",
      PEDESTRIAN_AND_BICYCLE,
      very_low_traffic,
      very_low_traffic
    );

    // segregated=no takes' precedence if there is no "segregated" key. There is no penalty for a tag mismatch
    props.setProperties(
      "highway=cycleway;foot=designated;segregated=no",
      PEDESTRIAN_AND_BICYCLE,
      mixed_cycleway,
      mixed_cycleway
    );
    props.setProperties(
      "highway=cycleway;foot=designated;segregated=yes",
      PEDESTRIAN_AND_BICYCLE,
      dedicated_cycleway,
      dedicated_cycleway
    );
    props.setProperties(
      "highway=cycleway;foot=designated;segregated=yes;lanes=2",
      BICYCLE,
      dual_lane_or_oneway_cycleway,
      dual_lane_or_oneway_cycleway
    );
    props.setProperties(
      "highway=path;foot=designated;bicycle=designated;segregated=no",
      PEDESTRIAN_AND_BICYCLE,
      mixed_cycleway,
      mixed_cycleway
    );
    props.setProperties(
      "highway=path;foot=designated;bicycle=designated;segregated=yes",
      PEDESTRIAN_AND_BICYCLE,
      dedicated_cycleway,
      dedicated_cycleway
    );
    // "motor_vehicle=destination" indicates unwanted car traffic, signposted "Kjøring til eiendommene tillatt"
    props.setProperties(
      "highway=cycleway;foot=designated;segregated=*;motor_vehicle=destination",
      PEDESTRIAN_AND_BICYCLE,
      very_low_traffic,
      very_low_traffic
    );
    props.setProperties(
      "highway=path;foot=designated;bicycle=designated;segregated=*;motor_vehicle=destination",
      PEDESTRIAN_AND_BICYCLE,
      very_low_traffic,
      very_low_traffic
    );

    //relation properties are copied over to ways
    props.setProperties("lcn=yes|rcn=yes|ncn=yes", ALL, 0.8, 0.8, true);

    props.setProperties("highway=busway", PEDESTRIAN_AND_BICYCLE, low_traffic, low_traffic);
    props.setProperties("highway=track", PEDESTRIAN_AND_BICYCLE, 1.1, 1.1);
    props.setProperties("highway=bridleway", PEDESTRIAN_AND_BICYCLE, 1.1, 1.1);
    props.setProperties("highway=path", PEDESTRIAN_AND_BICYCLE, 1.5, 1.5);
    props.setProperties("highway=steps", PEDESTRIAN);
    props.setProperties("highway=corridor", PEDESTRIAN);
    props.setProperties("highway=footway;indoor=yes", PEDESTRIAN);
    props.setProperties("highway=platform", PEDESTRIAN);
    props.setProperties("public_transport=platform", PEDESTRIAN);

    props.setProperties("smoothness=intermediate", ALL, 1.5, 1.5, true);
    props.setProperties("smoothness=bad", ALL, 2, 2, true);
    props.setProperties("highway=*;smoothness=very_bad", PEDESTRIAN);
    props.setProperties("highway=*;smoothness=horrible", NONE);
    props.setProperties("highway=*;smoothness=very_horrible", NONE);
    props.setProperties("highway=*;smoothness=impassable", NONE);

    props.setProperties("highway=*;mtb:scale=1", PEDESTRIAN);
    props.setProperties("highway=*;mtb:scale=2", PEDESTRIAN);
    props.setProperties("highway=*;mtb:scale=3", NONE);
    props.setProperties("highway=*;mtb:scale=4", NONE);
    props.setProperties("highway=*;mtb:scale=5", NONE);
    props.setProperties("highway=*;mtb:scale=6", NONE);

    props.setProperties(
      "highway=track;tracktype=grade1",
      PEDESTRIAN_AND_BICYCLE,
      very_low_traffic,
      very_low_traffic
    );
    props.setProperties("highway=track;tracktype=grade2", PEDESTRIAN_AND_BICYCLE, 1.1, 1.1);
    props.setProperties("highway=track;tracktype=grade3", PEDESTRIAN_AND_BICYCLE, 1.5, 1.5);
    props.setProperties("highway=track;tracktype=grade4", PEDESTRIAN);
    props.setProperties("highway=track;tracktype=grade5", PEDESTRIAN);

    props.setProperties("highway=path;trail_visibility=bad", NONE);
    props.setProperties("highway=path;trail_visibility=no", NONE);
    props.setProperties("highway=path;trail_visibility=low", NONE);
    props.setProperties("highway=path;trail_visibility=poor", NONE);

    props.setProperties("highway=path;sac_scale=mountain_hiking", NONE);
    props.setProperties("highway=path;sac_scale=demanding_mountain_hiking", NONE);
    props.setProperties("highway=path;sac_scale=alpine_hiking", NONE);
    props.setProperties("highway=path;sac_scale=demanding_alpine_hiking", NONE);
    props.setProperties("highway=path;sac_scale=difficult_alpine_hiking", NONE);

    // paved but unfavorable
    props.setProperties("surface=grass_paver", ALL, 1.2, 1.2, true);
    props.setProperties("surface=sett", ALL, 1.2, 1.2, true);
    props.setProperties("surface=cobblestone", ALL, 1.2, 1.2, true);
    props.setProperties("surface=unhewn_cobblestone", ALL, 1.5, 1.5, true);
    // Can be slick if wet, but otherwise not unfavorable to bikes
    props.setProperties("surface=metal_grid", ALL, 1.2, 1.2, true);
    props.setProperties("surface=metal", ALL, 1.2, 1.2, true);

    // unpaved
    props.setProperties("surface=unpaved", ALL, 1.2, 1.2, true);
    props.setProperties("surface=compacted", ALL, 1.2, 1.2, true);
    props.setProperties("surface=fine_gravel", ALL, 1.3, 1.3, true);
    props.setProperties("surface=pebblestone", ALL, 1.3, 1.3, true);
    props.setProperties("surface=gravel", ALL, 1.3, 1.3, true);
    props.setProperties("surface=woodchip", ALL, 1.5, 1.5, true);
    props.setProperties("surface=ground", ALL, 1.5, 1.5, true);
    props.setProperties("surface=dirt", ALL, 1.5, 1.5, true);
    props.setProperties("surface=earth", ALL, 1.5, 1.5, true);
    props.setProperties("surface=grass", ALL, 1.5, 1.5, true);
    props.setProperties("surface=mud", ALL, 2, 2, true);
    props.setProperties("surface=sand", ALL, 2, 2, true);

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
