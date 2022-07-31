package org.opentripplanner.graph_builder.module.osm;

import static org.opentripplanner.graph_builder.module.osm.WayPropertiesBuilder.of;
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

    props.setProperties("highway=motorway", of(CAR));
    props.setProperties("highway=motorway_link", of(CAR));

    // Do not walk on "Motortrafikkvei" ("motorvei klasse b")
    props.setProperties("highway=trunk;motorroad=yes", of(CAR));
    props.setProperties("highway=trunk_link;motorroad=yes", of(CAR));

    props.setProperties("highway=primary;motorroad=yes", of(CAR));
    props.setProperties("highway=primary_link;motorroad=yes", of(CAR));

    /* "highway=trunk" roads ("Riksveier") are often single carriageway, and not only legal for bicycles,
    but assumed to be  much safer than the  default profile (Except when the speedlimt exceeds 90). */
    props.setProperties("highway=trunk", of(ALL).bicycleSafety(high_traffic));
    props.setProperties("highway=trunk_link", of(ALL).bicycleSafety(high_traffic));
    // Discourage cycling on roads with no infrastructure for neither walking nor cycling
    props.setProperties(
      "highway=trunk;foot=no",
      of(BICYCLE_AND_CAR).bicycleSafety(very_high_traffic)
    );
    props.setProperties(
      "highway=trunk_link;foot=no",
      of(BICYCLE_AND_CAR).bicycleSafety(very_high_traffic)
    );
    // Discourage cycling on trunk road tunnels
    props.setProperties("highway=trunk;tunnel=yes", of(CAR).bicycleSafety(very_high_traffic));
    props.setProperties("highway=trunk_link;tunnel=yes", of(CAR).bicycleSafety(very_high_traffic));
    props.setProperties("highway=trunk;maxspeed=90", of(ALL).bicycleSafety(very_high_traffic));
    props.setProperties("highway=trunk_link;maxspeed=90", of(ALL).bicycleSafety(very_high_traffic));
    props.setProperties("highway=trunk;maxspeed=60", of(ALL).bicycleSafety(medium_high_traffic));
    props.setProperties(
      "highway=trunk_link;maxspeed=60",
      of(ALL).bicycleSafety(medium_high_traffic)
    );
    props.setProperties("highway=trunk;maxspeed=50", of(ALL).bicycleSafety(medium_traffic));
    props.setProperties("highway=trunk_link;maxspeed=50", of(ALL).bicycleSafety(medium_traffic));
    props.setProperties("highway=trunk;maxspeed=40", of(ALL).bicycleSafety(medium_traffic));
    props.setProperties("highway=trunk_link;maxspeed=40", of(ALL).bicycleSafety(medium_traffic));

    props.setProperties("highway=primary", of(ALL).bicycleSafety(high_traffic));
    props.setProperties("highway=primary_link", of(ALL).bicycleSafety(high_traffic));
    // Discourage cycling on roads with no infrastructure for neither walking nor cycling
    props.setProperties(
      "highway=primary;foot=no",
      of(BICYCLE_AND_CAR).bicycleSafety(very_high_traffic)
    );
    props.setProperties(
      "highway=primary_link;foot=no",
      of(BICYCLE_AND_CAR).bicycleSafety(very_high_traffic)
    );
    props.setProperties("highway=primary;maxspeed=90", of(ALL).bicycleSafety(very_high_traffic));
    props.setProperties(
      "highway=primary_link;maxspeed=90",
      of(ALL).bicycleSafety(very_high_traffic)
    );
    props.setProperties("highway=primary;maxspeed=60", of(ALL).bicycleSafety(medium_high_traffic));
    props.setProperties(
      "highway=primary_link;maxspeed=60",
      of(ALL).bicycleSafety(medium_high_traffic)
    );
    props.setProperties("highway=primary;maxspeed=50", of(ALL).bicycleSafety(medium_traffic));
    props.setProperties("highway=primary_link;maxspeed=50", of(ALL).bicycleSafety(medium_traffic));
    props.setProperties("highway=primary;maxspeed=40", of(ALL).bicycleSafety(medium_traffic));
    props.setProperties("highway=primary_link;maxspeed=40", of(ALL).bicycleSafety(medium_traffic));
    props.setProperties("highway=primary;maxspeed=30", of(ALL).bicycleSafety(low_traffic));
    props.setProperties("highway=primary_link;maxspeed=30", of(ALL).bicycleSafety(low_traffic));

    props.setProperties("highway=secondary", of(ALL).bicycleSafety(medium_high_traffic));
    props.setProperties("highway=secondary_link", of(ALL).bicycleSafety(medium_high_traffic));
    // Discourage cycling on roads with no infrastructure for neither walking nor cycling
    props.setProperties(
      "highway=secondary;foot=no",
      of(BICYCLE_AND_CAR).bicycleSafety(very_high_traffic)
    );
    props.setProperties(
      "highway=secondary_link;foot=no",
      of(BICYCLE_AND_CAR).bicycleSafety(very_high_traffic)
    );
    props.setProperties("highway=secondary;maxspeed=60", of(ALL).bicycleSafety(medium_traffic));
    props.setProperties(
      "highway=secondary_link;maxspeed=60",
      of(ALL).bicycleSafety(medium_traffic)
    );
    props.setProperties("highway=secondary;maxspeed=50", of(ALL).bicycleSafety(medium_low_traffic));
    props.setProperties(
      "highway=secondary_link;maxspeed=50",
      of(ALL).bicycleSafety(medium_low_traffic)
    );
    props.setProperties("highway=secondary;maxspeed=40", of(ALL).bicycleSafety(medium_low_traffic));
    props.setProperties(
      "highway=secondary_link;maxspeed=40",
      of(ALL).bicycleSafety(medium_low_traffic)
    );
    props.setProperties("highway=secondary;maxspeed=30", of(ALL).bicycleSafety(low_traffic));
    props.setProperties("highway=secondary_link;maxspeed=30", of(ALL).bicycleSafety(low_traffic));

    props.setProperties("highway=tertiary", of(ALL).bicycleSafety(medium_low_traffic));
    props.setProperties("highway=tertiary_link", of(ALL).bicycleSafety(medium_low_traffic));
    // Discourage cycling on roads with no infrastructure for neither walking nor cycling
    props.setProperties(
      "highway=tertiary;foot=no",
      of(BICYCLE_AND_CAR).bicycleSafety(very_high_traffic)
    );
    props.setProperties(
      "highway=tertiary_link;foot=no",
      of(BICYCLE_AND_CAR).bicycleSafety(very_high_traffic)
    );
    props.setProperties("highway=tertiary;maxspeed=80", of(ALL).bicycleSafety(medium_high_traffic));
    props.setProperties(
      "highway=tertiary_link;maxspeed=80",
      of(ALL).bicycleSafety(medium_high_traffic)
    );
    props.setProperties("highway=tertiary;maxspeed=70", of(ALL).bicycleSafety(medium_high_traffic));
    props.setProperties(
      "highway=tertiary_link;maxspeed=70",
      of(ALL).bicycleSafety(medium_high_traffic)
    );
    props.setProperties("highway=tertiary;maxspeed=40", of(ALL).bicycleSafety(low_traffic));
    props.setProperties("highway=tertiary_link;maxspeed=40", of(ALL).bicycleSafety(low_traffic));
    props.setProperties("highway=tertiary;maxspeed=30", of(ALL).bicycleSafety(low_traffic));
    props.setProperties("highway=tertiary_link;maxspeed=30", of(ALL).bicycleSafety(low_traffic));

    props.setProperties("highway=unclassified", of(ALL).bicycleSafety(low_traffic));
    // Discourage cycling on roads with no infrastructure for neither walking nor cycling
    props.setProperties(
      "highway=unclassified;foot=no",
      of(BICYCLE_AND_CAR).bicycleSafety(medium_traffic)
    );
    // These access tags indicates low traffic
    props.setProperties(
      "highway=unclassified;motor_vehicle=no",
      of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic)
    );
    props.setProperties(
      "highway=unclassified;motor_vehicle=private",
      of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic)
    );
    props.setProperties(
      "highway=unclassified;motor_vehicle=permit",
      of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic)
    );
    props.setProperties(
      "highway=unclassified;motor_vehicle=destination",
      of(ALL).bicycleSafety(very_low_traffic)
    );
    props.setProperties(
      "highway=unclassified;maxspeed=70",
      of(ALL).bicycleSafety(medium_high_traffic)
    );
    props.setProperties(
      "highway=unclassified;maxspeed=80",
      of(ALL).bicycleSafety(medium_high_traffic)
    );

    props.setProperties("highway=residential", of(ALL).bicycleSafety(low_traffic));
    // These access tags indicates low traffic
    props.setProperties(
      "highway=residential;motor_vehicle=no",
      of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic)
    );
    props.setProperties(
      "highway=residential;motor_vehicle=private",
      of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic)
    );
    props.setProperties(
      "highway=residential;motor_vehicle=permit",
      of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic)
    );
    props.setProperties(
      "highway=residential;motor_vehicle=destination",
      of(ALL).bicycleSafety(very_low_traffic)
    );

    props.setProperties("highway=service", of(ALL).bicycleSafety(low_traffic));
    // Discourage cycling on roads with no infrastructure for neither walking nor cycling
    props.setProperties(
      "highway=service;foot=no",
      of(BICYCLE_AND_CAR).bicycleSafety(medium_traffic)
    );
    // These access tags indicates low traffic
    props.setProperties(
      "highway=service;motor_vehicle=no",
      of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic)
    );
    props.setProperties(
      "highway=service;motor_vehicle=private",
      of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic)
    );
    props.setProperties(
      "highway=service;motor_vehicle=permit",
      of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic)
    );
    props.setProperties(
      "highway=service;motor_vehicle=destination",
      of(ALL).bicycleSafety(very_low_traffic)
    );
    // Cycling around reversing cars on a parking lot feels unsafe
    props.setProperties(
      "highway=service;service=parking_aisle",
      of(ALL).bicycleSafety(medium_traffic)
    );
    props.setProperties(
      "highway=service;service=drive-through",
      of(ALL).bicycleSafety(medium_traffic)
    );

    /* bicycle infrastructure */
    props.setProperties(
      "highway=trunk;cycleway=track;maxspeed=*",
      of(ALL).bicycleSafety(dual_lane_or_oneway_cycleway)
    );
    props.setProperties(
      "highway=trunk_link;cycleway=track;maxspeed=*",
      of(ALL).bicycleSafety(dual_lane_or_oneway_cycleway)
    );
    props.setProperties(
      "highway=primary;cycleway=track;maxspeed=*",
      of(ALL).bicycleSafety(dual_lane_or_oneway_cycleway)
    );
    props.setProperties(
      "highway=primary_link;cycleway=track;maxspeed=*",
      of(ALL).bicycleSafety(dual_lane_or_oneway_cycleway)
    );
    props.setProperties(
      "highway=secondary;cycleway=track;maxspeed=*",
      of(ALL).bicycleSafety(dual_lane_or_oneway_cycleway)
    );
    props.setProperties(
      "highway=secondary_link;cycleway=track;maxspeed=*",
      of(ALL).bicycleSafety(dual_lane_or_oneway_cycleway)
    );
    props.setProperties(
      "highway=tertiary;cycleway=track;maxspeed=*",
      of(ALL).bicycleSafety(dual_lane_or_oneway_cycleway)
    );
    props.setProperties(
      "highway=tertiary_link;cycleway=track;maxspeed=*",
      of(ALL).bicycleSafety(dual_lane_or_oneway_cycleway)
    );
    props.setProperties(
      "highway=unclassified;cycleway=track;maxspeed=*",
      of(ALL).bicycleSafety(dual_lane_or_oneway_cycleway)
    );
    props.setProperties(
      "highway=residential;cycleway=track;maxspeed=*",
      of(ALL).bicycleSafety(dual_lane_or_oneway_cycleway)
    );
    props.setProperties(
      "highway=living_street;cycleway=track;maxspeed=*",
      of(ALL).bicycleSafety(dual_lane_or_oneway_cycleway)
    );

    props.setProperties(
      "highway=trunk;cycleway=lane;maxspeed=*",
      of(ALL).bicycleSafety(cycle_lane_medium_traffic)
    );
    props.setProperties(
      "highway=trunk_link;cycleway=lane;maxspeed=*",
      of(ALL).bicycleSafety(cycle_lane_medium_traffic)
    );
    props.setProperties(
      "highway=primary;cycleway=lane;maxspeed=*",
      of(ALL).bicycleSafety(cycle_lane_medium_traffic)
    );
    props.setProperties(
      "highway=primary_link;cycleway=lane;maxspeed=*",
      of(ALL).bicycleSafety(cycle_lane_medium_traffic)
    );
    props.setProperties(
      "highway=secondary;cycleway=lane;maxspeed=*",
      of(ALL).bicycleSafety(cycle_lane_medium_traffic)
    );
    props.setProperties(
      "highway=secondary_link;cycleway=lane;maxspeed=*",
      of(ALL).bicycleSafety(cycle_lane_medium_traffic)
    );
    props.setProperties(
      "highway=secondary;cycleway=lane;maxspeed=40",
      of(ALL).bicycleSafety(cycle_lane_low_traffic)
    );
    props.setProperties(
      "highway=secondary_link;cycleway=lane;maxspeed=40",
      of(ALL).bicycleSafety(cycle_lane_low_traffic)
    );
    props.setProperties(
      "highway=secondary;cycleway=lane;maxspeed=30",
      of(ALL).bicycleSafety(cycle_lane_low_traffic)
    );
    props.setProperties(
      "highway=secondary_link;cycleway=lane;maxspeed=30",
      of(ALL).bicycleSafety(cycle_lane_low_traffic)
    );
    props.setProperties(
      "highway=tertiary;cycleway=lane;maxspeed=*",
      of(ALL).bicycleSafety(cycle_lane_medium_traffic)
    );
    props.setProperties(
      "highway=tertiary_link;cycleway=lane;maxspeed=*",
      of(ALL).bicycleSafety(cycle_lane_medium_traffic)
    );
    props.setProperties(
      "highway=tertiary;cycleway=lane;maxspeed=40",
      of(ALL).bicycleSafety(cycle_lane_low_traffic)
    );
    props.setProperties(
      "highway=tertiary_link;cycleway=lane;maxspeed=40",
      of(ALL).bicycleSafety(cycle_lane_low_traffic)
    );
    props.setProperties(
      "highway=tertiary;cycleway=lane;maxspeed=30",
      of(ALL).bicycleSafety(cycle_lane_low_traffic)
    );
    props.setProperties(
      "highway=tertiary_link;cycleway=lane;maxspeed=30",
      of(ALL).bicycleSafety(cycle_lane_low_traffic)
    );
    props.setProperties(
      "highway=unclassified;cycleway=lane;maxspeed=*",
      of(ALL).bicycleSafety(cycle_lane_medium_traffic)
    );
    props.setProperties(
      "highway=residential;cycleway=lane;maxspeed=*",
      of(ALL).bicycleSafety(cycle_lane_low_traffic)
    );
    props.setProperties(
      "highway=living_street;cycleway=lane;maxspeed=*",
      of(ALL).bicycleSafety(cycle_lane_low_traffic)
    );

    /* opposite */
    props.setProperties(
      "highway=trunk;cycleway=opposite_track;maxspeed=*",
      of(ALL).bicycleSafety(high_traffic, dual_lane_or_oneway_cycleway)
    );
    props.setProperties(
      "highway=trunk_link;cycleway=opposite_track;maxspeed=*",
      of(ALL).bicycleSafety(high_traffic, dual_lane_or_oneway_cycleway)
    );
    props.setProperties(
      "highway=primary;cycleway=opposite_track;maxspeed=*",
      of(ALL).bicycleSafety(high_traffic, dual_lane_or_oneway_cycleway)
    );
    props.setProperties(
      "highway=primary_link;cycleway=opposite_track;maxspeed=*",
      of(ALL).bicycleSafety(high_traffic, dual_lane_or_oneway_cycleway)
    );
    props.setProperties(
      "highway=secondary;cycleway=opposite_track;maxspeed=*",
      of(ALL).bicycleSafety(medium_high_traffic, dual_lane_or_oneway_cycleway)
    );
    props.setProperties(
      "highway=secondary_link;cycleway=opposite_track;maxspeed=*",
      of(ALL).bicycleSafety(medium_high_traffic, dual_lane_or_oneway_cycleway)
    );
    props.setProperties(
      "highway=tertiary;cycleway=opposite_track;maxspeed=*",
      of(ALL).bicycleSafety(low_traffic, dual_lane_or_oneway_cycleway)
    );
    props.setProperties(
      "highway=tertiary_link;cycleway=opposite_track;maxspeed=*",
      of(ALL).bicycleSafety(low_traffic, dual_lane_or_oneway_cycleway)
    );
    props.setProperties(
      "highway=unclassified;cycleway=opposite_track;maxspeed=*",
      of(ALL).bicycleSafety(low_traffic, dual_lane_or_oneway_cycleway)
    );
    props.setProperties(
      "highway=residential;cycleway=opposite_track;maxspeed=*",
      of(ALL).bicycleSafety(low_traffic, dual_lane_or_oneway_cycleway)
    );
    props.setProperties(
      "highway=living_street;cycleway=opposite_track;maxspeed=*",
      of(ALL).bicycleSafety(low_traffic, dual_lane_or_oneway_cycleway)
    );

    props.setProperties(
      "highway=trunk;cycleway=opposite_lane;maxspeed=*",
      of(ALL).bicycleSafety(high_traffic, cycle_lane_medium_traffic)
    );
    props.setProperties(
      "highway=trunk_link;cycleway=opposite_lane;maxspeed=*",
      of(ALL).bicycleSafety(high_traffic, cycle_lane_medium_traffic)
    );
    props.setProperties(
      "highway=primary;cycleway=opposite_lane;maxspeed=*",
      of(ALL).bicycleSafety(high_traffic, cycle_lane_medium_traffic)
    );
    props.setProperties(
      "highway=primary_link;cycleway=opposite_lane;maxspeed=*",
      of(ALL).bicycleSafety(high_traffic, cycle_lane_medium_traffic)
    );
    props.setProperties(
      "highway=secondary;cycleway=opposite_lane;maxspeed=*",
      of(ALL).bicycleSafety(medium_high_traffic, cycle_lane_medium_traffic)
    );
    props.setProperties(
      "highway=secondary_link;cycleway=opposite_lane;maxspeed=*",
      of(ALL).bicycleSafety(medium_high_traffic, cycle_lane_medium_traffic)
    );
    props.setProperties(
      "highway=tertiary;cycleway=opposite_lane;maxspeed=*",
      of(ALL).bicycleSafety(low_traffic, cycle_lane_low_traffic)
    );
    props.setProperties(
      "highway=tertiary_link;cycleway=opposite_lane;maxspeed=*",
      of(ALL).bicycleSafety(low_traffic, cycle_lane_low_traffic)
    );
    props.setProperties(
      "highway=unclassified;cycleway=opposite_lane;maxspeed=*",
      of(ALL).bicycleSafety(low_traffic, cycle_lane_low_traffic)
    );
    props.setProperties(
      "highway=residential;cycleway=opposite_lane;maxspeed=*",
      of(ALL).bicycleSafety(low_traffic, cycle_lane_low_traffic)
    );
    props.setProperties(
      "highway=living_street;cycleway=opposite_lane;maxspeed=*",
      of(ALL).bicycleSafety(low_traffic, cycle_lane_low_traffic)
    );

    /* Pedestrian, living and cyclestreet */
    props.setProperties("highway=living_street", of(ALL).bicycleSafety(low_traffic));
    props.setProperties("highway=pedestrian", of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.2));
    props.setProperties(
      "highway=residential;cyclestreet=yes;motor_vehicle=*",
      of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic)
    );

    props.setProperties(
      "highway=footway",
      of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(dedicated_footway)
    );
    // "motor_vehicle=destination" indicates unwanted car traffic, signposted "Kjøring til eiendommene tillatt"
    props.setProperties(
      "highway=footway;motor_vehicle=destination",
      of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic)
    );

    props.setProperties(
      "highway=footway;footway=sidewalk",
      of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(sidewalk)
    );
    props.setProperties(
      "highway=footway;footway=crossing",
      of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(footway_crossing)
    );
    props.setProperties(
      "highway=cycleway;footway=sidewalk",
      of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(dedicated_footway)
    );
    props.setProperties(
      "highway=cycleway;footway=crossing",
      of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(footway_crossing)
    );
    props.setProperties(
      "highway=cycleway;cycleway=crossing",
      of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(footway_crossing)
    );

    props.setProperties(
      "highway=cycleway",
      of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(dedicated_cycleway)
    );
    props.setProperties(
      "highway=cycleway;lanes=2",
      of(BICYCLE).bicycleSafety(dual_lane_or_oneway_cycleway)
    );
    props.setProperties(
      "highway=cycleway;oneway=yes",
      of(BICYCLE).bicycleSafety(dual_lane_or_oneway_cycleway)
    );
    // "motor_vehicle=destination" indicates unwanted car traffic, signposted "Kjøring til eiendommene tillatt"
    props.setProperties(
      "highway=cycleway;motor_vehicle=destination",
      of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic)
    );

    // segregated=no takes' precedence if there is no "segregated" key. There is no penalty for a tag mismatch
    props.setProperties(
      "highway=cycleway;foot=designated;segregated=no",
      of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(mixed_cycleway)
    );
    props.setProperties(
      "highway=cycleway;foot=designated;segregated=yes",
      of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(dedicated_cycleway)
    );
    props.setProperties(
      "highway=cycleway;foot=designated;segregated=yes;lanes=2",
      of(BICYCLE).bicycleSafety(dual_lane_or_oneway_cycleway)
    );
    props.setProperties(
      "highway=path;foot=designated;bicycle=designated;segregated=no",
      of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(mixed_cycleway)
    );
    props.setProperties(
      "highway=path;foot=designated;bicycle=designated;segregated=yes",
      of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(dedicated_cycleway)
    );
    // "motor_vehicle=destination" indicates unwanted car traffic, signposted "Kjøring til eiendommene tillatt"
    props.setProperties(
      "highway=cycleway;foot=designated;segregated=*;motor_vehicle=destination",
      of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic)
    );
    props.setProperties(
      "highway=path;foot=designated;bicycle=designated;segregated=*;motor_vehicle=destination",
      of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic)
    );

    //relation properties are copied over to ways
    props.setMixinProperties("lcn=yes|rcn=yes|ncn=yes", of(ALL).bicycleSafety(0.8));

    props.setProperties("highway=busway", of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(low_traffic));
    props.setProperties("highway=track", of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.1));
    props.setProperties("highway=bridleway", of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.1));
    props.setProperties("highway=path", of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.5));
    props.setProperties("highway=steps", of(PEDESTRIAN));
    props.setProperties("highway=corridor", of(PEDESTRIAN));
    props.setProperties("highway=footway;indoor=yes", of(PEDESTRIAN));
    props.setProperties("highway=platform", of(PEDESTRIAN));
    props.setProperties("public_transport=platform", of(PEDESTRIAN));

    props.setMixinProperties("smoothness=intermediate", of(ALL).bicycleSafety(1.5));
    props.setMixinProperties("smoothness=bad", of(ALL).bicycleSafety(2));
    props.setProperties("highway=*;smoothness=very_bad", of(PEDESTRIAN));
    props.setProperties("highway=*;smoothness=horrible", of(NONE));
    props.setProperties("highway=*;smoothness=very_horrible", of(NONE));
    props.setProperties("highway=*;smoothness=impassable", of(NONE));

    props.setProperties("highway=*;mtb:scale=1", of(PEDESTRIAN));
    props.setProperties("highway=*;mtb:scale=2", of(PEDESTRIAN));
    props.setProperties("highway=*;mtb:scale=3", of(NONE));
    props.setProperties("highway=*;mtb:scale=4", of(NONE));
    props.setProperties("highway=*;mtb:scale=5", of(NONE));
    props.setProperties("highway=*;mtb:scale=6", of(NONE));

    props.setProperties(
      "highway=track;tracktype=grade1",
      of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic)
    );
    props.setProperties(
      "highway=track;tracktype=grade2",
      of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.1)
    );
    props.setProperties(
      "highway=track;tracktype=grade3",
      of(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.5)
    );
    props.setProperties("highway=track;tracktype=grade4", of(PEDESTRIAN));
    props.setProperties("highway=track;tracktype=grade5", of(PEDESTRIAN));

    props.setProperties("highway=path;trail_visibility=bad", of(NONE));
    props.setProperties("highway=path;trail_visibility=no", of(NONE));
    props.setProperties("highway=path;trail_visibility=low", of(NONE));
    props.setProperties("highway=path;trail_visibility=poor", of(NONE));

    props.setProperties("highway=path;sac_scale=mountain_hiking", of(NONE));
    props.setProperties("highway=path;sac_scale=demanding_mountain_hiking", of(NONE));
    props.setProperties("highway=path;sac_scale=alpine_hiking", of(NONE));
    props.setProperties("highway=path;sac_scale=demanding_alpine_hiking", of(NONE));
    props.setProperties("highway=path;sac_scale=difficult_alpine_hiking", of(NONE));

    // paved but unfavorable
    props.setMixinProperties("surface=grass_paver", of(ALL).bicycleSafety(1.2));
    props.setMixinProperties("surface=sett", of(ALL).bicycleSafety(1.2));
    props.setMixinProperties("surface=cobblestone", of(ALL).bicycleSafety(1.2));
    props.setMixinProperties("surface=unhewn_cobblestone", of(ALL).bicycleSafety(1.5));
    // Can be slick if wet, but otherwise not unfavorable to bikes
    props.setMixinProperties("surface=metal_grid", of(ALL).bicycleSafety(1.2));
    props.setMixinProperties("surface=metal", of(ALL).bicycleSafety(1.2));

    // unpaved
    props.setMixinProperties("surface=unpaved", of(ALL).bicycleSafety(1.2));
    props.setMixinProperties("surface=compacted", of(ALL).bicycleSafety(1.2));
    props.setMixinProperties("surface=fine_gravel", of(ALL).bicycleSafety(1.3));
    props.setMixinProperties("surface=pebblestone", of(ALL).bicycleSafety(1.3));
    props.setMixinProperties("surface=gravel", of(ALL).bicycleSafety(1.3));
    props.setMixinProperties("surface=woodchip", of(ALL).bicycleSafety(1.5));
    props.setMixinProperties("surface=ground", of(ALL).bicycleSafety(1.5));
    props.setMixinProperties("surface=dirt", of(ALL).bicycleSafety(1.5));
    props.setMixinProperties("surface=earth", of(ALL).bicycleSafety(1.5));
    props.setMixinProperties("surface=grass", of(ALL).bicycleSafety(1.5));
    props.setMixinProperties("surface=mud", of(ALL).bicycleSafety(2));
    props.setMixinProperties("surface=sand", of(ALL).bicycleSafety(2));

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
