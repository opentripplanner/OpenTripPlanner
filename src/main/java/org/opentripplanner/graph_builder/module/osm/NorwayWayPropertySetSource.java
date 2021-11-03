package org.opentripplanner.graph_builder.module.osm;

import static org.opentripplanner.graph_builder.module.osm.WayPropertySetSource.DrivingDirection.RIGHT_HAND_TRAFFIC;

import org.opentripplanner.routing.core.intersection_model.IntersectionTraversalCostModel;
import org.opentripplanner.routing.core.intersection_model.NorwayIntersectionTraversalCostModel;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

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
    var medium_low_traffic = 1.46;
    var low_traffic = 0.97;
    var very_low_traffic = 0.92;

    var cycle_lane_medium_traffic = 0.87;
    var cycle_lane_low_traffic = 0.77;

    var dedicated_footway = 0.9;
    var footway_crossing = 1.2;
    var mixed_cycleway = 0.8;
    var dedicated_cycleway = 0.7;
    var dual_lane_or_oneway_cycleway = 0.6;

    props.setProperties("highway=motorway", StreetTraversalPermission.CAR);
    props.setProperties("highway=motorway_link", StreetTraversalPermission.CAR);

    // Do not walk on "Motortrafikkvei" ("motorvei klasse b")
    props.setProperties("highway=trunk;motorroad=yes", StreetTraversalPermission.CAR);
    props.setProperties("highway=trunk_link;motorroad=yes", StreetTraversalPermission.CAR);

    props.setProperties("highway=primary;motorroad=yes", StreetTraversalPermission.CAR);
    props.setProperties("highway=primary_link;motorroad=yes", StreetTraversalPermission.CAR);

    /* "highway=trunk" roads ("Riksveier") are often single carriageway, and not only legal for bicycles,
    but assumed to be  much safer than the  default profile (Except when the speedlimt exceeds 90). */
    props.setProperties(
            "highway=trunk", StreetTraversalPermission.ALL,
            high_traffic, high_traffic
    );
    props.setProperties(
            "highway=trunk_link", StreetTraversalPermission.ALL,
            high_traffic, high_traffic
    );
    // foot=no indicates heavy traffic, and is considered less safe for bicycles.
    props.setProperties(
            "highway=trunk;foot=no", StreetTraversalPermission.BICYCLE_AND_CAR,
            very_high_traffic, very_high_traffic
    );
    props.setProperties(
            "highway=trunk_link;foot=no", StreetTraversalPermission.BICYCLE_AND_CAR,
            very_high_traffic, very_high_traffic
    );
    // Don't recommend walking in trunk road tunnels (although actually legal unless explicitly forbidden)
    props.setProperties(
            "highway=trunk;tunnel=yes", StreetTraversalPermission.CAR
    );
    props.setProperties(
            "highway=trunk_link;tunnel=yes", StreetTraversalPermission.CAR
    );
    props.setProperties(
            "highway=trunk;maxspeed=90", StreetTraversalPermission.ALL,
            very_high_traffic, very_high_traffic
    );
    props.setProperties(
            "highway=trunk_link;maxspeed=90", StreetTraversalPermission.ALL,
            very_high_traffic, very_high_traffic
    );
    props.setProperties(
            "highway=trunk;maxspeed=60", StreetTraversalPermission.ALL,
            medium_high_traffic, medium_high_traffic
    );
    props.setProperties(
            "highway=trunk_link;maxspeed=60", StreetTraversalPermission.ALL,
            medium_high_traffic, medium_high_traffic
    );
    props.setProperties(
            "highway=trunk;maxspeed=50", StreetTraversalPermission.ALL,
            medium_traffic, medium_traffic
    );
    props.setProperties(
            "highway=trunk_link;maxspeed=50", StreetTraversalPermission.ALL,
            medium_traffic, medium_traffic
    );
    props.setProperties(
            "highway=trunk;maxspeed=40", StreetTraversalPermission.ALL,
            medium_traffic, medium_traffic
    );
    props.setProperties(
            "highway=trunk_link;maxspeed=40", StreetTraversalPermission.ALL,
            medium_traffic, medium_traffic
    );

    props.setProperties(
            "highway=primary", StreetTraversalPermission.ALL,
            high_traffic, high_traffic
    );
    props.setProperties(
            "highway=primary_link", StreetTraversalPermission.ALL,
            high_traffic, high_traffic
    );
    props.setProperties(
            "highway=primary;foot=no", StreetTraversalPermission.BICYCLE_AND_CAR,
            very_high_traffic, very_high_traffic
    );
    props.setProperties(
            "highway=primary_link;foot=no", StreetTraversalPermission.BICYCLE_AND_CAR,
            very_high_traffic, very_high_traffic)
    ;
    props.setProperties(
            "highway=primary;maxspeed=90", StreetTraversalPermission.ALL,
            very_high_traffic, very_high_traffic
    );
    props.setProperties(
            "highway=primary_link;maxspeed=90", StreetTraversalPermission.ALL,
            very_high_traffic, very_high_traffic
    );
    props.setProperties(
            "highway=primary;maxspeed=60", StreetTraversalPermission.ALL,
            medium_high_traffic, medium_high_traffic
    );
    props.setProperties(
            "highway=primary_link;maxspeed=60", StreetTraversalPermission.ALL,
            medium_high_traffic, medium_high_traffic
    );
    props.setProperties(
            "highway=primary;maxspeed=50", StreetTraversalPermission.ALL,
            medium_traffic, medium_traffic
    );
    props.setProperties(
            "highway=primary_link;maxspeed=50", StreetTraversalPermission.ALL,
            medium_traffic, medium_traffic
    );
    props.setProperties(
            "highway=primary;maxspeed=40", StreetTraversalPermission.ALL,
            medium_traffic, medium_traffic
    );
    props.setProperties(
            "highway=primary_link;maxspeed=40", StreetTraversalPermission.ALL,
            medium_traffic, medium_traffic
    );
    props.setProperties(
            "highway=primary;maxspeed=30", StreetTraversalPermission.ALL,
            very_low_traffic, very_low_traffic
    );
    props.setProperties(
            "highway=primary_link;maxspeed=30", StreetTraversalPermission.ALL,
            very_low_traffic, very_low_traffic
    );

    props.setProperties(
            "highway=secondary", StreetTraversalPermission.ALL,
            medium_high_traffic, medium_high_traffic
    );
    props.setProperties(
            "highway=secondary_link", StreetTraversalPermission.ALL,
            medium_high_traffic, medium_high_traffic
    );
    props.setProperties(
            "highway=secondary;foot=no", StreetTraversalPermission.BICYCLE_AND_CAR,
            very_high_traffic, very_high_traffic
    );
    props.setProperties(
            "highway=secondary_link;foot=no", StreetTraversalPermission.BICYCLE_AND_CAR,
            very_high_traffic, very_high_traffic
    );
    props.setProperties(
            "highway=secondary;maxspeed=60", StreetTraversalPermission.ALL,
            medium_traffic, medium_traffic
    );
    props.setProperties(
            "highway=secondary_link;maxspeed=60", StreetTraversalPermission.ALL,
            medium_traffic, medium_traffic
    );
    props.setProperties(
            "highway=secondary;maxspeed=50", StreetTraversalPermission.ALL,
            medium_low_traffic, medium_low_traffic
    );
    props.setProperties(
            "highway=secondary_link;maxspeed=50", StreetTraversalPermission.ALL,
            medium_low_traffic, medium_low_traffic
    );
    props.setProperties(
            "highway=secondary;maxspeed=40", StreetTraversalPermission.ALL,
            medium_low_traffic, medium_low_traffic
    );
    props.setProperties(
            "highway=secondary_link;maxspeed=40", StreetTraversalPermission.ALL,
            medium_low_traffic, medium_low_traffic
    );
    props.setProperties(
            "highway=secondary;maxspeed=30", StreetTraversalPermission.ALL,
            very_low_traffic, very_low_traffic
    );
    props.setProperties(
            "highway=secondary_link;maxspeed=30", StreetTraversalPermission.ALL,
            very_low_traffic, very_low_traffic
    );

    props.setProperties(
            "highway=tertiary", StreetTraversalPermission.ALL,
            low_traffic, low_traffic
    );
    props.setProperties(
            "highway=tertiary_link", StreetTraversalPermission.ALL,
            low_traffic, low_traffic
    );
    props.setProperties(
            "highway=tertiary;foot=no", StreetTraversalPermission.BICYCLE_AND_CAR,
            very_high_traffic, very_high_traffic
    );
    props.setProperties(
            "highway=tertiary_link;foot=no", StreetTraversalPermission.BICYCLE_AND_CAR,
            very_high_traffic, very_high_traffic
    );
    props.setProperties(
            "highway=tertiary;maxspeed=80", StreetTraversalPermission.ALL,
            medium_high_traffic, medium_high_traffic
    );
    props.setProperties(
            "highway=tertiary_link;maxspeed=80", StreetTraversalPermission.ALL,
            medium_high_traffic, medium_high_traffic
    );
    props.setProperties(
            "highway=tertiary;maxspeed=70", StreetTraversalPermission.ALL,
            medium_high_traffic, medium_high_traffic
    );
    props.setProperties(
            "highway=tertiary_link;maxspeed=70", StreetTraversalPermission.ALL,
            medium_high_traffic, medium_high_traffic
    );
    props.setProperties(
            "highway=tertiary;maxspeed=30", StreetTraversalPermission.ALL,
            very_low_traffic, very_low_traffic
    );
    props.setProperties(
            "highway=tertiary_link;maxspeed=30", StreetTraversalPermission.ALL,
            very_low_traffic, very_low_traffic
    );

    props.setProperties(
            "highway=unclassified", StreetTraversalPermission.ALL,
            low_traffic, low_traffic
    );
    props.setProperties(
            "highway=unclassified;foot=no", StreetTraversalPermission.BICYCLE_AND_CAR,
            medium_traffic, medium_traffic
    );
    // These access tags indicates low traffic
    props.setProperties(
            "highway=unclassified;motor_vehicle=no", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
            very_low_traffic, very_low_traffic
    );
    props.setProperties(
            "highway=unclassified;motor_vehicle=private", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
            very_low_traffic, very_low_traffic
    );
    props.setProperties(
            "highway=unclassified;motor_vehicle=permit", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
            very_low_traffic, very_low_traffic
    );
    props.setProperties(
            "highway=unclassified;motor_vehicle=destination", StreetTraversalPermission.ALL,
            very_low_traffic, very_low_traffic
    );
    props.setProperties(
            "highway=unclassified;maxspeed=70", StreetTraversalPermission.ALL,
            medium_high_traffic, medium_high_traffic
    );
    props.setProperties(
            "highway=unclassified;maxspeed=80", StreetTraversalPermission.ALL,
            medium_high_traffic, medium_high_traffic
    );

    props.setProperties(
            "highway=residential", StreetTraversalPermission.ALL,
            low_traffic, low_traffic
    );
    // These access tags indicates low traffic
    props.setProperties(
            "highway=residential;motor_vehicle=no", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
            very_low_traffic, very_low_traffic
    );
    props.setProperties(
            "highway=residential;motor_vehicle=private", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
            very_low_traffic, very_low_traffic
    );
    props.setProperties(
            "highway=residential;motor_vehicle=permit", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
            very_low_traffic, very_low_traffic
    );
    props.setProperties(
            "highway=residential;motor_vehicle=destination", StreetTraversalPermission.ALL,
            very_low_traffic, very_low_traffic
    );

    props.setProperties(
            "highway=service", StreetTraversalPermission.ALL,
            low_traffic, low_traffic
    );
    props.setProperties(
            "highway=service;foot=no", StreetTraversalPermission.BICYCLE_AND_CAR,
            medium_traffic, medium_traffic
    );
    // These access tags indicates low traffic
    props.setProperties(
            "highway=service;motor_vehicle=no", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
            low_traffic, low_traffic
    );
    props.setProperties(
            "highway=service;motor_vehicle=private", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
            low_traffic, low_traffic
    );
    props.setProperties(
            "highway=service;motor_vehicle=permit", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
            low_traffic, low_traffic
    );
    props.setProperties(
            "highway=service;motor_vehicle=destination", StreetTraversalPermission.ALL,
            low_traffic, low_traffic
    );
    // Cycling around reversing cars on a parking lot feels unsafe
    props.setProperties(
            "highway=service;service=parking_aisle", StreetTraversalPermission.ALL,
            medium_traffic, medium_traffic
    );
    props.setProperties(
            "highway=service;service=drive-through", StreetTraversalPermission.ALL,
            medium_traffic, medium_traffic
    );

    /* bicycle infrastructure */
    props.setProperties(
            "highway=trunk;cycleway=track;maxspeed=*", StreetTraversalPermission.ALL,
            dual_lane_or_oneway_cycleway, dual_lane_or_oneway_cycleway
    );
    props.setProperties(
            "highway=trunk_link;cycleway=track;maxspeed=*", StreetTraversalPermission.ALL,
            dual_lane_or_oneway_cycleway, dual_lane_or_oneway_cycleway
    );
    props.setProperties(
            "highway=primary;cycleway=track;maxspeed=*", StreetTraversalPermission.ALL,
            dual_lane_or_oneway_cycleway, dual_lane_or_oneway_cycleway
    );
    props.setProperties(
            "highway=primary_link;cycleway=track;maxspeed=*", StreetTraversalPermission.ALL,
            dual_lane_or_oneway_cycleway, dual_lane_or_oneway_cycleway
    );
    props.setProperties(
            "highway=secondary;cycleway=track;maxspeed=*", StreetTraversalPermission.ALL,
            dual_lane_or_oneway_cycleway, dual_lane_or_oneway_cycleway
    );
    props.setProperties(
            "highway=secondary_link;cycleway=track;maxspeed=*", StreetTraversalPermission.ALL,
            dual_lane_or_oneway_cycleway, dual_lane_or_oneway_cycleway
    );
    props.setProperties(
            "highway=tertiary;cycleway=track;maxspeed=*", StreetTraversalPermission.ALL,
            dual_lane_or_oneway_cycleway, dual_lane_or_oneway_cycleway
    );
    props.setProperties(
            "highway=tertiary_link;cycleway=track;maxspeed=*", StreetTraversalPermission.ALL,
            dual_lane_or_oneway_cycleway, dual_lane_or_oneway_cycleway
    );
    props.setProperties(
            "highway=unclassified;cycleway=track;maxspeed=*", StreetTraversalPermission.ALL,
            dual_lane_or_oneway_cycleway, dual_lane_or_oneway_cycleway
    );
    props.setProperties(
            "highway=residential;cycleway=track;maxspeed=*", StreetTraversalPermission.ALL,
            dual_lane_or_oneway_cycleway, dual_lane_or_oneway_cycleway
    );
    props.setProperties(
            "highway=living_street;cycleway=track;maxspeed=*", StreetTraversalPermission.ALL,
            dual_lane_or_oneway_cycleway, dual_lane_or_oneway_cycleway
    );

    props.setProperties(
            "highway=trunk;cycleway=lane;maxspeed=*", StreetTraversalPermission.ALL,
            cycle_lane_medium_traffic, cycle_lane_medium_traffic
    );
    props.setProperties(
            "highway=trunk_link;cycleway=lane;maxspeed=*", StreetTraversalPermission.ALL,
            cycle_lane_medium_traffic, cycle_lane_medium_traffic
    );
    props.setProperties(
            "highway=primary;cycleway=lane;maxspeed=*", StreetTraversalPermission.ALL,
            cycle_lane_medium_traffic, cycle_lane_medium_traffic
    );
    props.setProperties(
            "highway=primary_link;cycleway=lane;maxspeed=*", StreetTraversalPermission.ALL,
            cycle_lane_medium_traffic, cycle_lane_medium_traffic
    );
    props.setProperties(
            "highway=secondary;cycleway=lane;maxspeed=*", StreetTraversalPermission.ALL,
            cycle_lane_medium_traffic, cycle_lane_medium_traffic
    );
    props.setProperties(
            "highway=secondary_link;cycleway=lane;maxspeed=*", StreetTraversalPermission.ALL,
            cycle_lane_medium_traffic, cycle_lane_medium_traffic
    );
    props.setProperties(
            "highway=secondary;cycleway=lane;maxspeed=40", StreetTraversalPermission.ALL,
            cycle_lane_low_traffic, cycle_lane_low_traffic
    );
    props.setProperties(
            "highway=secondary_link;cycleway=lane;maxspeed=40", StreetTraversalPermission.ALL,
            cycle_lane_low_traffic, cycle_lane_low_traffic
    );
    props.setProperties(
            "highway=secondary;cycleway=lane;maxspeed=30", StreetTraversalPermission.ALL,
            cycle_lane_low_traffic, cycle_lane_low_traffic
    );
    props.setProperties(
            "highway=secondary_link;cycleway=lane;maxspeed=30", StreetTraversalPermission.ALL,
            cycle_lane_low_traffic, cycle_lane_low_traffic
    );
    props.setProperties(
            "highway=tertiary;cycleway=lane;maxspeed=*", StreetTraversalPermission.ALL,
            cycle_lane_medium_traffic, cycle_lane_medium_traffic
    );
    props.setProperties(
            "highway=tertiary_link;cycleway=lane;maxspeed=*", StreetTraversalPermission.ALL,
            cycle_lane_medium_traffic, cycle_lane_medium_traffic
    );
    props.setProperties(
            "highway=tertiary;cycleway=lane;maxspeed=40", StreetTraversalPermission.ALL,
            cycle_lane_low_traffic, cycle_lane_low_traffic
    );
    props.setProperties(
            "highway=tertiary_link;cycleway=lane;maxspeed=40", StreetTraversalPermission.ALL,
            cycle_lane_low_traffic, cycle_lane_low_traffic
    );
    props.setProperties(
            "highway=tertiary;cycleway=lane;maxspeed=30", StreetTraversalPermission.ALL,
            cycle_lane_low_traffic, cycle_lane_low_traffic
    );
    props.setProperties(
            "highway=tertiary_link;cycleway=lane;maxspeed=30", StreetTraversalPermission.ALL,
            cycle_lane_low_traffic, cycle_lane_low_traffic
    );
    props.setProperties(
            "highway=unclassified;cycleway=lane;maxspeed=*", StreetTraversalPermission.ALL,
            cycle_lane_medium_traffic, cycle_lane_medium_traffic
    );
    props.setProperties(
            "highway=residential;cycleway=lane;maxspeed=*", StreetTraversalPermission.ALL,
            cycle_lane_low_traffic, cycle_lane_low_traffic
    );
    props.setProperties(
            "highway=living_street;cycleway=lane;maxspeed=*", StreetTraversalPermission.ALL,
            cycle_lane_low_traffic, cycle_lane_low_traffic
    );

    /* opposite */
    props.setProperties(
            "highway=trunk;cycleway=opposite_track;maxspeed=*", StreetTraversalPermission.ALL,
            high_traffic, dual_lane_or_oneway_cycleway
    );
    props.setProperties(
            "highway=trunk_link;cycleway=opposite_track;maxspeed=*", StreetTraversalPermission.ALL,
            high_traffic, dual_lane_or_oneway_cycleway
    );
    props.setProperties(
            "highway=primary;cycleway=opposite_track;maxspeed=*", StreetTraversalPermission.ALL,
            high_traffic, dual_lane_or_oneway_cycleway
    );
    props.setProperties(
            "highway=primary_link;cycleway=opposite_track;maxspeed=*", StreetTraversalPermission.ALL,
            high_traffic, dual_lane_or_oneway_cycleway
    );
    props.setProperties(
            "highway=secondary;cycleway=opposite_track;maxspeed=*", StreetTraversalPermission.ALL,
            medium_high_traffic, dual_lane_or_oneway_cycleway
    );
    props.setProperties(
            "highway=secondary_link;cycleway=opposite_track;maxspeed=*", StreetTraversalPermission.ALL,
            medium_high_traffic, dual_lane_or_oneway_cycleway
    );
    props.setProperties(
            "highway=tertiary;cycleway=opposite_track;maxspeed=*", StreetTraversalPermission.ALL,
            low_traffic, dual_lane_or_oneway_cycleway
    );
    props.setProperties(
            "highway=tertiary_link;cycleway=opposite_track;maxspeed=*", StreetTraversalPermission.ALL,
            low_traffic, dual_lane_or_oneway_cycleway
    );
    props.setProperties(
            "highway=unclassified;cycleway=opposite_track;maxspeed=*", StreetTraversalPermission.ALL,
            low_traffic, dual_lane_or_oneway_cycleway
    );
    props.setProperties(
            "highway=residential;cycleway=opposite_track;maxspeed=*", StreetTraversalPermission.ALL,
            low_traffic, dual_lane_or_oneway_cycleway
    );
    props.setProperties(
            "highway=living_street;cycleway=opposite_track;maxspeed=*", StreetTraversalPermission.ALL,
            low_traffic, dual_lane_or_oneway_cycleway
    );

    props.setProperties(
            "highway=trunk;cycleway=opposite_lane;maxspeed=*", StreetTraversalPermission.ALL,
            high_traffic, cycle_lane_medium_traffic
    );
    props.setProperties(
            "highway=trunk_link;cycleway=opposite_lane;maxspeed=*", StreetTraversalPermission.ALL,
            high_traffic, cycle_lane_medium_traffic
    );
    props.setProperties(
            "highway=primary;cycleway=opposite_lane;maxspeed=*", StreetTraversalPermission.ALL,
            high_traffic, cycle_lane_medium_traffic
    );
    props.setProperties(
            "highway=primary_link;cycleway=opposite_lane;maxspeed=*", StreetTraversalPermission.ALL,
            high_traffic, cycle_lane_medium_traffic
    );
    props.setProperties(
            "highway=secondary;cycleway=opposite_lane;maxspeed=*", StreetTraversalPermission.ALL,
            medium_high_traffic, cycle_lane_medium_traffic
    );
    props.setProperties(
            "highway=secondary_link;cycleway=opposite_lane;maxspeed=*", StreetTraversalPermission.ALL,
            medium_high_traffic, cycle_lane_medium_traffic
    );
    props.setProperties(
            "highway=tertiary;cycleway=opposite_lane;maxspeed=*", StreetTraversalPermission.ALL,
            low_traffic, cycle_lane_low_traffic
    );
    props.setProperties(
            "highway=tertiary_link;cycleway=opposite_lane;maxspeed=*", StreetTraversalPermission.ALL,
            low_traffic, cycle_lane_low_traffic
    );
    props.setProperties(
            "highway=unclassified;cycleway=opposite_lane;maxspeed=*", StreetTraversalPermission.ALL,
            low_traffic, cycle_lane_low_traffic
    );
    props.setProperties(
            "highway=residential;cycleway=opposite_lane;maxspeed=*", StreetTraversalPermission.ALL,
            low_traffic, cycle_lane_low_traffic
    );
    props.setProperties(
            "highway=living_street;cycleway=opposite_lane;maxspeed=*", StreetTraversalPermission.ALL,
            low_traffic, cycle_lane_low_traffic
    );

    /* Pedestrian, living and cyclestreet */
    props.setProperties(
            "highway=living_street",
            StreetTraversalPermission.ALL,
            low_traffic, low_traffic
    );
    props.setProperties(
            "highway=pedestrian",
            StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
            1.2, 1.2
    );
    props.setProperties(
            "highway=pedestrian;bicycle=designated",
            StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
            dedicated_cycleway, dedicated_cycleway
    );
    props.setProperties(
            "highway=residential;cyclestreet=yes;motor_vehicle=*",
            StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
            dedicated_cycleway, dedicated_cycleway
    );

    props.setProperties(
            "highway=footway",
            StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
            dedicated_footway, dedicated_footway
    );
    // "motor_vehicle=destination" indicates unwanted car traffic, signposted "Kjøring til eiendommene tillatt"
    props.setProperties(
            "highway=footway;motor_vehicle=destination",
            StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
            low_traffic, low_traffic
    );

    props.setProperties(
            "highway=footway;footway=sidewalk",
            StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
            dedicated_footway, dedicated_footway
    );
    props.setProperties(
            "highway=footway;footway=crossing",
            StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
            footway_crossing, footway_crossing
    );
    props.setProperties(
            "highway=cycleway;footway=sidewalk",
            StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
            dedicated_footway, dedicated_footway
    );
    props.setProperties(
            "highway=cycleway;footway=crossing",
            StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
            footway_crossing, footway_crossing
    );
    props.setProperties(
            "highway=cycleway;cycleway=sidewalk",
            StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
            dedicated_footway, dedicated_footway
    );
    props.setProperties(
            "highway=cycleway;cycleway=crossing",
            StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
            footway_crossing, footway_crossing
    );

    props.setProperties(
            "highway=cycleway", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
            dedicated_cycleway, dedicated_cycleway
    );
    props.setProperties(
            "highway=cycleway;lanes=2", StreetTraversalPermission.BICYCLE,
            dual_lane_or_oneway_cycleway, dual_lane_or_oneway_cycleway
    );
    props.setProperties(
            "highway=cycleway;oneway=yes", StreetTraversalPermission.BICYCLE,
            dual_lane_or_oneway_cycleway, dual_lane_or_oneway_cycleway
    );
    // "motor_vehicle=destination" indicates unwanted car traffic, signposted "Kjøring til eiendommene tillatt"
    props.setProperties(
            "highway=cycleway;motor_vehicle=destination", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
            very_low_traffic, very_low_traffic
    );

    // segregated=no takes' precedence if there is no "segregated" key. There is no penalty for a tag mismatch
    props.setProperties(
            "highway=cycleway;foot=designated;segregated=no",
            StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
            mixed_cycleway, mixed_cycleway
    );
    props.setProperties(
            "highway=cycleway;foot=designated;segregated=yes",
            StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
            dedicated_cycleway, dedicated_cycleway
    );
    props.setProperties(
            "highway=cycleway;foot=designated;segregated=yes;lanes=2",
            StreetTraversalPermission.BICYCLE,
            dual_lane_or_oneway_cycleway, dual_lane_or_oneway_cycleway
    );
    props.setProperties(
            "highway=path;foot=designated;bicycle=designated;segregated=no",
            StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
            mixed_cycleway, mixed_cycleway
    );
    props.setProperties(
            "highway=path;foot=designated;bicycle=designated;segregated=yes",
            StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
            dedicated_cycleway, dedicated_cycleway
    );
    props.setProperties(
            "highway=cycleway;foot=designated;segregated=*;motor_vehicle=destination",
            StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
            very_low_traffic, very_low_traffic
    );
    props.setProperties(
            "highway=path;foot=designated;bicycle=designated;segregated=*;motor_vehicle=destination",
            StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
            very_low_traffic, very_low_traffic
    );

    //relation properties are copied over to ways
    props.setProperties(
            "lcn=yes|rcn=yes|ncn=yes", StreetTraversalPermission.ALL, 0.8, 0.8, true
    );

    props.setProperties("highway=busway", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, low_traffic, low_traffic);
    props.setProperties("highway=track", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.1, 1.1);
    props.setProperties("highway=bridleway", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.9, 0.9);
    props.setProperties("highway=path", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.9, 0.9);
    props.setProperties("highway=steps", StreetTraversalPermission.PEDESTRIAN);
    props.setProperties("highway=corridor", StreetTraversalPermission.PEDESTRIAN);
    props.setProperties("highway=footway;indoor=yes", StreetTraversalPermission.PEDESTRIAN);
    props.setProperties("highway=platform", StreetTraversalPermission.PEDESTRIAN);
    props.setProperties("public_transport=platform", StreetTraversalPermission.PEDESTRIAN);

    props.setProperties("smoothness=intermediate", StreetTraversalPermission.ALL, 1.1, 1.1, true);
    props.setProperties("smoothness=bad", StreetTraversalPermission.ALL, 2, 2, true);
    props.setProperties("highway=*;smoothness=very_bad", StreetTraversalPermission.PEDESTRIAN);
    props.setProperties("highway=*;smoothness=horrible", StreetTraversalPermission.NONE);
    props.setProperties("highway=*;smoothness=very_horrible", StreetTraversalPermission.NONE);
    props.setProperties("highway=*;smoothness=impassable", StreetTraversalPermission.NONE);

    props.setProperties("highway=*;tracktype=grade1", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.9, 0.9);
    props.setProperties("highway=*;tracktype=grade2", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.1, 1.1);
    props.setProperties("highway=*;tracktype=grade3", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.2, 1.2);
    props.setProperties("highway=*;tracktype=grade3", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.5, 1.5);
    props.setProperties("highway=*;tracktype=grade4", StreetTraversalPermission.PEDESTRIAN);
    props.setProperties("highway=*;tracktype=grade5", StreetTraversalPermission.PEDESTRIAN);

    props.setProperties("highway=path;trail_visibility=bad", StreetTraversalPermission.NONE);
    props.setProperties("highway=path;trail_visibility=no", StreetTraversalPermission.NONE);
    props.setProperties("highway=path;trail_visibility=low", StreetTraversalPermission.NONE);
    props.setProperties("highway=path;trail_visibility=poor", StreetTraversalPermission.NONE);

    props.setProperties("highway=path;sac_scale=mountain_hiking", StreetTraversalPermission.NONE);
    props.setProperties("highway=path;sac_scale=demanding_mountain_hiking", StreetTraversalPermission.NONE);
    props.setProperties("highway=path;sac_scale=alpine_hiking", StreetTraversalPermission.NONE);
    props.setProperties("highway=path;sac_scale=demanding_alpine_hiking", StreetTraversalPermission.NONE);
    props.setProperties("highway=path;sac_scale=difficult_alpine_hiking", StreetTraversalPermission.NONE);

    props.setProperties("highway=path;mtb:scale=1", StreetTraversalPermission.PEDESTRIAN);
    props.setProperties("highway=path;mtb:scale=2", StreetTraversalPermission.PEDESTRIAN);
    props.setProperties("highway=path;mtb:scale=3", StreetTraversalPermission.NONE);
    props.setProperties("highway=path;mtb:scale=4", StreetTraversalPermission.NONE);
    props.setProperties("highway=path;mtb:scale=5", StreetTraversalPermission.NONE);
    props.setProperties("highway=path;mtb:scale=6", StreetTraversalPermission.NONE);

    // paved but unfavorable
    props.setProperties("surface=grass_paver", StreetTraversalPermission.ALL, 1.2, 1.2, true);
    props.setProperties("surface=sett", StreetTraversalPermission.ALL, 1.2, 1.2, true);
    props.setProperties("surface=cobblestone", StreetTraversalPermission.ALL, 1.2, 1.2, true);
    props.setProperties("surface=unhewn_cobblestone", StreetTraversalPermission.ALL, 1.5, 1.5, true);
    // Can be slick if wet, but otherwise not unfavorable to bikes
    props.setProperties("surface=metal_grid", StreetTraversalPermission.ALL, 1.2, 1.2, true);
    props.setProperties("surface=metal", StreetTraversalPermission.ALL, 1.2, 1.2, true);

    // unpaved
    props.setProperties("surface=unpaved", StreetTraversalPermission.ALL, 1.2, 1.2, true);
    props.setProperties("surface=compacted", StreetTraversalPermission.ALL, 1.2, 1.2, true);
    props.setProperties("surface=fine_gravel", StreetTraversalPermission.ALL, 1.3, 1.3, true);
    props.setProperties("surface=pebblestone", StreetTraversalPermission.ALL, 1.3, 1.3, true);
    props.setProperties("surface=gravel", StreetTraversalPermission.ALL, 1.3, 1.3, true);
    props.setProperties("surface=woodchip", StreetTraversalPermission.ALL, 1.5, 1.5, true);
    props.setProperties("surface=ground", StreetTraversalPermission.ALL, 1.5, 1.5, true);
    props.setProperties("surface=dirt", StreetTraversalPermission.ALL, 1.5, 1.5, true);
    props.setProperties("surface=earth", StreetTraversalPermission.ALL, 1.5, 1.5, true);
    props.setProperties("surface=grass", StreetTraversalPermission.ALL, 1.5, 1.5, true);
    props.setProperties("surface=mud", StreetTraversalPermission.ALL, 2, 2, true);
    props.setProperties("surface=sand", StreetTraversalPermission.ALL, 2, 2, true);

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
