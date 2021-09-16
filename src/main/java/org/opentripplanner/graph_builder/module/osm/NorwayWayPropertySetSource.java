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

    props.setProperties("highway=motorway", StreetTraversalPermission.CAR);
    props.setProperties("highway=motorway_link", StreetTraversalPermission.CAR);
    // Patch missing vehicle=no check
    props.setProperties("highway=motorway_link;vehicle=no", StreetTraversalPermission.NONE);
    props.setProperties("highway=motorway_link=yes;vehicle=no", StreetTraversalPermission.NONE);

    // Do not walk on "Motortrafikkvei" ("motorvei klasse b")
    props.setProperties("highway=trunk;motorroad=yes", StreetTraversalPermission.CAR);
    props.setProperties("highway=trunk_link;motorroad=yes", StreetTraversalPermission.CAR);
    // Patch missing vehicle=no check
    props.setProperties("highway=trunk;motorroad=yes;vehicle=no", StreetTraversalPermission.NONE);
    props.setProperties("highway=trunk_link;motorroad=yes;vehicle=no", StreetTraversalPermission.NONE);

    props.setProperties("highway=primary;motorroad=yes", StreetTraversalPermission.CAR);
    props.setProperties("highway=primary_link;motorroad=yes", StreetTraversalPermission.CAR);
    // Patch missing vehicle=no check
    props.setProperties("highway=primary_link;motorroad=yes;vehicle=no", StreetTraversalPermission.NONE);
    props.setProperties("highway=primary_link;motorroad=yes;vehicle=no", StreetTraversalPermission.NONE);

    /* "highway=trunk" roads ("Riksveier") are often single carriageway, and not only legal for bicycles,
    but assumed to be  much safer than the  default profile (Except when the speedlimt exceeds 90). */
    props.setProperties("highway=trunk", StreetTraversalPermission.ALL, 2.25, 2.25);
    props.setProperties("highway=trunk_link", StreetTraversalPermission.ALL, 2.25, 2.25);
    // foot=no indicates heavy traffic, and is considered less safe for bicycles.
    props.setProperties("highway=trunk;foot=no", StreetTraversalPermission.BICYCLE_AND_CAR, 7, 7);
    // Patch missing vehicle=no check
    props.setProperties("highway=trunk;vehicle=no", StreetTraversalPermission.PEDESTRIAN);
    props.setProperties("highway=trunk_link;vehicle=no", StreetTraversalPermission.PEDESTRIAN);
    // Don't recommend walking in trunk road tunnels (although actually legal unless explicitly forbidden)
    props.setProperties("highway=trunk;tunnel=yes", StreetTraversalPermission.CAR);
    props.setProperties("highway=trunk_link;tunnel=yes", StreetTraversalPermission.CAR);
    props.setProperties("highway=trunk;maxspeed=90", StreetTraversalPermission.ALL, 7, 7);
    props.setProperties("highway=trunk_link;maxspeed=90", StreetTraversalPermission.ALL, 7, 7);
    props.setProperties("highway=trunk;maxspeed=60", StreetTraversalPermission.ALL, 2.06, 2.06);
    props.setProperties("highway=trunk_link;maxspeed=60", StreetTraversalPermission.ALL, 2.06, 2.06);
    props.setProperties("highway=trunk;maxspeed=50", StreetTraversalPermission.ALL, 1.5, 1.5);
    props.setProperties("highway=trunk_link;maxspeed=50", StreetTraversalPermission.ALL, 1.5, 1.5);
    props.setProperties("highway=trunk;maxspeed=40", StreetTraversalPermission.ALL, 1.5, 1.5);
    props.setProperties("highway=trunk_link;maxspeed=40", StreetTraversalPermission.ALL, 1.5, 1.5);

    props.setProperties("highway=primary", StreetTraversalPermission.ALL, 2.25, 2.25);
    props.setProperties("highway=primary_link", StreetTraversalPermission.ALL, 2.25, 2.25);
    props.setProperties("highway=primary;foot=no", StreetTraversalPermission.BICYCLE_AND_CAR, 7, 7);
    props.setProperties("highway=primary_link;foot=no", StreetTraversalPermission.BICYCLE_AND_CAR, 7, 7);
    // Patch missing vehicle=no check
    props.setProperties("highway=primary;vehicle=no", StreetTraversalPermission.PEDESTRIAN, 8, 8);
    props.setProperties("highway=primary_link;vehicle=no", StreetTraversalPermission.PEDESTRIAN, 8, 8);
    props.setProperties("highway=primary;maxspeed=90", StreetTraversalPermission.ALL, 7, 7);
    props.setProperties("highway=primary_link;maxspeed=90", StreetTraversalPermission.ALL, 7, 7);
    props.setProperties("highway=primary;maxspeed=60", StreetTraversalPermission.ALL, 1.50, 1.50);
    props.setProperties("highway=primary_link;maxspeed=60", StreetTraversalPermission.ALL, 1.50, 1.50);
    props.setProperties("highway=primary;maxspeed=50", StreetTraversalPermission.ALL, 1.46, 1.46);
    props.setProperties("highway=primary_link;maxspeed=50", StreetTraversalPermission.ALL, 1.46, 1.46);
    props.setProperties("highway=primary;maxspeed=40", StreetTraversalPermission.ALL, 1.46, 1.46);
    props.setProperties("highway=primary_link;maxspeed=40", StreetTraversalPermission.ALL, 1.46, 1.46);
    props.setProperties("highway=primary;maxspeed=30", StreetTraversalPermission.ALL, 0.92, 0.92);
    props.setProperties("highway=primary_link;maxspeed=30", StreetTraversalPermission.ALL, 0.92, 0.92);

    props.setProperties("highway=secondary", StreetTraversalPermission.ALL, 2.06, 2.06);
    props.setProperties("highway=secondary_link", StreetTraversalPermission.ALL, 2.06, 2.06);
    props.setProperties("highway=secondary;foot=no", StreetTraversalPermission.BICYCLE_AND_CAR, 7, 7);
    props.setProperties("highway=secondary_link;foot=no", StreetTraversalPermission.BICYCLE_AND_CAR, 7, 7);
    // Patch missing vehicle=no check
    props.setProperties("highway=secondary;vehicle=no", StreetTraversalPermission.PEDESTRIAN, 8, 8);
    props.setProperties("highway=secondary_link;vehicle=no", StreetTraversalPermission.PEDESTRIAN, 8, 8);
    props.setProperties("highway=secondary;maxspeed=60", StreetTraversalPermission.ALL, 1.50, 1.50);
    props.setProperties("highway=secondary_link;maxspeed=60", StreetTraversalPermission.ALL, 1.50, 1.50);
    props.setProperties("highway=secondary;maxspeed=50", StreetTraversalPermission.ALL, 1.46, 1.46);
    props.setProperties("highway=secondary_link;maxspeed=50", StreetTraversalPermission.ALL, 1.46, 1.46);
    props.setProperties("highway=secondary;maxspeed=40", StreetTraversalPermission.ALL, 1.46, 1.46);
    props.setProperties("highway=secondary_link;maxspeed=40", StreetTraversalPermission.ALL, 1.46, 1.46);
    props.setProperties("highway=secondary;maxspeed=30", StreetTraversalPermission.ALL, 0.92, 0.92);
    props.setProperties("highway=secondary_link;maxspeed=30", StreetTraversalPermission.ALL, 0.92, 0.92);

    props.setProperties("highway=tertiary", StreetTraversalPermission.ALL, 1.0, 1.0);
    props.setProperties("highway=tertiary_link", StreetTraversalPermission.ALL, 1.0, 1.0);
    props.setProperties("highway=tertiary;foot=no", StreetTraversalPermission.BICYCLE_AND_CAR, 7, 7);
    props.setProperties("highway=tertiary_link;foot=no", StreetTraversalPermission.BICYCLE_AND_CAR, 7, 7);
    // Patch missing vehicle=no check
    props.setProperties("highway=tertiary;vehicle=no", StreetTraversalPermission.PEDESTRIAN, 8, 8);
    props.setProperties("highway=tertiary_link;vehicle=no", StreetTraversalPermission.PEDESTRIAN, 8, 8);
    props.setProperties("highway=tertiary;maxspeed=60", StreetTraversalPermission.ALL, 0.97, 0.97);
    props.setProperties("highway=tertiary_link;maxspeed=60", StreetTraversalPermission.ALL, 0.97, 0.97);
    props.setProperties("highway=tertiary;maxspeed=50", StreetTraversalPermission.ALL, 0.92, 0.92);
    props.setProperties("highway=tertiary_link;maxspeed=50", StreetTraversalPermission.ALL, 0.92, 0.92);
    props.setProperties("highway=tertiary;maxspeed=40", StreetTraversalPermission.ALL, 0.92, 0.92);
    props.setProperties("highway=tertiary_link;maxspeed=40", StreetTraversalPermission.ALL, 0.92, 0.92);
    props.setProperties("highway=tertiary;maxspeed=30", StreetTraversalPermission.ALL, 0.92, 0.92);
    props.setProperties("highway=tertiary_link;maxspeed=30", StreetTraversalPermission.ALL, 0.92, 0.92);

    props.setProperties("highway=unclassified", StreetTraversalPermission.ALL, 0.97, 0.97);
    props.setProperties("highway=unclassified;foot=no", StreetTraversalPermission.BICYCLE_AND_CAR, 6, 6);
    // These access tags indicates low traffic
    props.setProperties("highway=unclassified;motor_vehicle=no", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.92, 0.92);
    props.setProperties("highway=unclassified;motor_vehicle=private", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.92, 0.92);
    props.setProperties("highway=unclassified;motor_vehicle=permit", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.92, 0.92);
    props.setProperties("highway=unclassified;motor_vehicle=destination", StreetTraversalPermission.ALL, 0.92, 0.92);
    props.setProperties("highway=unclassified;maxspeed=70", StreetTraversalPermission.ALL, 2.06, 2.06);
    props.setProperties("highway=unclassified;maxspeed=80", StreetTraversalPermission.ALL, 2.06, 2.06);

    props.setProperties("highway=residential", StreetTraversalPermission.ALL, 0.97, 0.97);
    // These access tags indicates low traffic
    props.setProperties("highway=residential;motor_vehicle=no", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.92, 0.92);
    props.setProperties("highway=residential;motor_vehicle=private", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.92, 0.92);
    props.setProperties("highway=residential;motor_vehicle=permit", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.92, 0.92);
    props.setProperties("highway=residential;motor_vehicle=destination", StreetTraversalPermission.ALL, 0.92, 0.92);

    props.setProperties("highway=service", StreetTraversalPermission.ALL, 0.97, 0.97);
    props.setProperties("highway=service;foot=no", StreetTraversalPermission.BICYCLE_AND_CAR, 1.5, 1.5);
    // These access tags indicates low traffic
    props.setProperties("highway=service;motor_vehicle=no", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.97, 0.97);
    props.setProperties("highway=service;motor_vehicle=private", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.97, 0.97);
    props.setProperties("highway=service;motor_vehicle=permit", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.97, 0.97);
    props.setProperties("highway=service;motor_vehicle=destination", StreetTraversalPermission.ALL, 0.97, 0.97);
    // Cycling around reversing cars on a parking lot feels unsafe
    props.setProperties("highway=service;service=parking_aisle", StreetTraversalPermission.ALL, 1.5, 1.5);
    props.setProperties("highway=service;service=drive-through", StreetTraversalPermission.ALL, 1.5, 1.5);

    /* bicycle infrastructure */
    props.setProperties("highway=trunk;cycleway=track;maxspeed=*", StreetTraversalPermission.ALL, 0.62, 0.62);
    props.setProperties("highway=trunk_link;cycleway=track;maxspeed=*", StreetTraversalPermission.ALL, 0.62, 0.62);
    props.setProperties("highway=primary;cycleway=track;maxspeed=*", StreetTraversalPermission.ALL, 0.62, 0.62);
    props.setProperties("highway=primary_link;cycleway=track;maxspeed=*", StreetTraversalPermission.ALL,0.62, 0.62);
    props.setProperties("highway=secondary;cycleway=track;maxspeed=*", StreetTraversalPermission.ALL,0.62, 0.62);
    props.setProperties("highway=secondary_link;cycleway=track;maxspeed=*", StreetTraversalPermission.ALL,0.62, 0.62);
    props.setProperties("highway=tertiary;cycleway=track;maxspeed=*", StreetTraversalPermission.ALL,0.62, 0.62);
    props.setProperties("highway=tertiary_link;cycleway=track;maxspeed=*", StreetTraversalPermission.ALL,0.62, 0.62);
    props.setProperties("highway=unclassified;cycleway=track;maxspeed=*", StreetTraversalPermission.ALL,0.62, 0.62);
    props.setProperties("highway=residential;cycleway=track;maxspeed=*", StreetTraversalPermission.ALL,0.62, 0.62);
    props.setProperties("highway=living_street;cycleway=track;maxspeed=*", StreetTraversalPermission.ALL,0.62, 0.62);

    props.setProperties("highway=trunk;cycleway=lane;maxspeed=*", StreetTraversalPermission.ALL, 0.87, 0.87);
    props.setProperties("highway=trunk_link;cycleway=lane;maxspeed=*", StreetTraversalPermission.ALL, 0.87, 0.87);
    props.setProperties("highway=primary;cycleway=lane;maxspeed=*", StreetTraversalPermission.ALL, 0.87, 0.87);
    props.setProperties("highway=primary_link;cycleway=lane;maxspeed=*", StreetTraversalPermission.ALL,0.87, 0.87);
    props.setProperties("highway=secondary;cycleway=lane;maxspeed=*", StreetTraversalPermission.ALL,0.87, 0.87);
    props.setProperties("highway=secondary_link;cycleway=lane;maxspeed=*", StreetTraversalPermission.ALL,0.87, 0.87);
    props.setProperties("highway=secondary;cycleway=lane;maxspeed=40", StreetTraversalPermission.ALL,0.77, 0.77);
    props.setProperties("highway=secondary;cycleway=lane;maxspeed=40", StreetTraversalPermission.ALL,0.77, 0.77);
    props.setProperties("highway=secondary;cycleway=lane;maxspeed=30", StreetTraversalPermission.ALL,0.77, 0.77);
    props.setProperties("highway=secondary;cycleway=lane;maxspeed=30", StreetTraversalPermission.ALL,0.77, 0.77);
    props.setProperties("highway=tertiary;cycleway=lane;maxspeed=*", StreetTraversalPermission.ALL,0.87, 0.87);
    props.setProperties("highway=tertiary_link;cycleway=lane;maxspeed=*", StreetTraversalPermission.ALL,0.87, 0.87);
    props.setProperties("highway=tertiary;cycleway=lane;maxspeed=40", StreetTraversalPermission.ALL,0.77, 0.77);
    props.setProperties("highway=tertiary_link;cycleway=lane;maxspeed=40", StreetTraversalPermission.ALL,0.77, 0.77);
    props.setProperties("highway=tertiary;cycleway=lane;maxspeed=30", StreetTraversalPermission.ALL,0.77, 0.77);
    props.setProperties("highway=tertiary_link;cycleway=lane;maxspeed=30", StreetTraversalPermission.ALL,0.77, 0.77);
    props.setProperties("highway=unclassified;cycleway=lane;maxspeed=*", StreetTraversalPermission.ALL,0.87, 0.87);
    props.setProperties("highway=residential;cycleway=lane;maxspeed=*", StreetTraversalPermission.ALL,0.77, 0.77);
    props.setProperties("highway=living_street;cycleway=lane;maxspeed=*", StreetTraversalPermission.ALL,0.77, 0.77);

    /* opposite */
    props.setProperties("highway=trunk;cycleway=opposite_track;maxspeed=*", StreetTraversalPermission.ALL, 2.25, 0.62);
    props.setProperties("highway=trunk_link;cycleway=opposite_track;maxspeed=*", StreetTraversalPermission.ALL, 2.25, 0.62);
    props.setProperties("highway=primary;cycleway=opposite_track;maxspeed=*", StreetTraversalPermission.ALL, 2.25, 0.62);
    props.setProperties("highway=primary_link;cycleway=opposite_track;maxspeed=*", StreetTraversalPermission.ALL,2.25, 0.62);
    props.setProperties("highway=secondary;cycleway=opposite_track;maxspeed=*", StreetTraversalPermission.ALL,2.06, 0.62);
    props.setProperties("highway=secondary_link;cycleway=opposite_track;maxspeed=*", StreetTraversalPermission.ALL,2.06, 0.62);
    props.setProperties("highway=tertiary;cycleway=opposite_track;maxspeed=*", StreetTraversalPermission.ALL,1, 0.62);
    props.setProperties("highway=tertiary_link;cycleway=opposite_track;maxspeed=*", StreetTraversalPermission.ALL,1, 0.62);
    props.setProperties("highway=unclassified;cycleway=opposite_track;maxspeed=*", StreetTraversalPermission.ALL,0.97, 0.62);
    props.setProperties("highway=residential;cycleway=opposite_track;maxspeed=*", StreetTraversalPermission.ALL,0.97, 0.62);
    props.setProperties("highway=living_street;cycleway=opposite_track;maxspeed=*", StreetTraversalPermission.ALL,0.97, 0.62);

    props.setProperties("highway=trunk;cycleway=opposite_lane;maxspeed=*", StreetTraversalPermission.ALL, 2.25, 0.87);
    props.setProperties("highway=trunk_link;cycleway=opposite_lane;maxspeed=*", StreetTraversalPermission.ALL, 2.25, 0.87);
    props.setProperties("highway=primary;cycleway=opposite_lane;maxspeed=*", StreetTraversalPermission.ALL, 2.25, 0.87);
    props.setProperties("highway=primary_link;cycleway=opposite_lane;maxspeed=*", StreetTraversalPermission.ALL,2.25, 0.87);
    props.setProperties("highway=secondary;cycleway=opposite_lane;maxspeed=*", StreetTraversalPermission.ALL, 2.06, 0.87);
    props.setProperties("highway=secondary_link;cycleway=opposite_lane;maxspeed=*", StreetTraversalPermission.ALL,2.06, 0.87);
    props.setProperties("highway=tertiary;cycleway=opposite_lane;maxspeed=*", StreetTraversalPermission.ALL,1, 0.87);
    props.setProperties("highway=tertiary_link;cycleway=opposite_lane;maxspeed=*", StreetTraversalPermission.ALL,1, 0.87);
    props.setProperties("highway=unclassified;cycleway=opposite_lane;maxspeed=*", StreetTraversalPermission.ALL,0.97, 0.87);
    props.setProperties("highway=residential;cycleway=opposite_lane;maxspeed=*", StreetTraversalPermission.ALL,0.97, 0.77);
    props.setProperties("highway=living_street;cycleway=opposite_lane;maxspeed=*", StreetTraversalPermission.ALL,0.97, 0.77);

    /* Pedestrian, living and cyclestreet */
    props.setProperties("highway=living_street", StreetTraversalPermission.ALL, 0.97, 0.97);
    props.setProperties("highway=pedestrian", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.2, 1.2);
    props.setProperties("highway=pedestrian;bicycle=designated", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.7, 0.7);
    props.setProperties("highway=residential;cyclestreet=yes;motor_vehicle=*", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.7, 0.7);

    props.setProperties("highway=footway", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.8, 0.8);
    // "motor_vehicle=destination" indicates unwanted car traffic, signposted "Kjøring til eiendommene tillatt"
    props.setProperties("highway=footway;motor_vehicle=destination", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.9,  0.9);

    props.setProperties("highway=footway;footway=sidewalk", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1, 1);
    props.setProperties("highway=footway;footway=crossing", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.2, 1.2);
    props.setProperties("highway=cycleway;footway=sidewalk", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.9, 0.9);
    props.setProperties("highway=cycleway;footway=crossing", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.2, 1.2);
    props.setProperties("highway=cycleway;cycleway=sidewalk", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.9, 0.9);
    props.setProperties("highway=cycleway;cycleway=crossing", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.2, 1.2);

    props.setProperties("highway=cycleway", StreetTraversalPermission.BICYCLE, 0.7, 0.7);
    props.setProperties("highway=cycleway;lanes=2", StreetTraversalPermission.BICYCLE, 0.6, 0.6);
    props.setProperties("highway=cycleway;oneway=yes", StreetTraversalPermission.BICYCLE,0.6,0.6);
    // "motor_vehicle=destination" indicates unwanted car traffic, signposted "Kjøring til eiendommene tillatt"
    props.setProperties("highway=cycleway;motor_vehicle=destination", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.9,  0.9);

    // segregated=no takes' precedence if there is no "segregated" key. There is no penalty for a tag mismatch
    props.setProperties("highway=cycleway;foot=designated;segregated=no", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.8, 0.8);
    props.setProperties("highway=cycleway;foot=designated;segregated=yes", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.7, 0.7);
    props.setProperties("highway=path;foot=designated;bicycle=designated;segregated=no", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.8,  0.8);
    props.setProperties("highway=path;foot=designated;bicycle=designated;segregated=yes", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.7,  0.7);
    props.setProperties("highway=cycleway;foot=designated;segregated=*;motor_vehicle=destination", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.9, 0.9);
    props.setProperties("highway=path;foot=designated;bicycle=designated;segregated=*;motor_vehicle=destination", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.9,  0.9);

    //relation properties are copied over to ways
    props.setProperties(
            "route=bicycle", StreetTraversalPermission.ALL, 0.8, 0.8, true
    );

    props.setProperties("highway=busway", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1,  1);
    props.setProperties("highway=track", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.1,  1.1);
    props.setProperties("highway=bridleway", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.9,  0.9);
    props.setProperties("highway=path", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.9,  0.9);
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

    // paved but unfavorable
    props.setProperties("surface=grass_paver", StreetTraversalPermission.ALL, 1.2, 1.2, true);
    props.setProperties("surface=sett", StreetTraversalPermission.ALL, 1.2, 1.2, true);
    props.setProperties("surface=cobblestone", StreetTraversalPermission.ALL, 1.2, 1.2, true);
    props.setProperties("surface=metal_grid", StreetTraversalPermission.ALL, 1.2, 1.2, true);
    props.setProperties("surface=unhewn_cobblestone", StreetTraversalPermission.ALL, 1.5, 1.5, true);
    // Can be slick if wet, but otherwise not unfavorable to bikes

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
    props.setProperties("surface=mud", StreetTraversalPermission.ALL, 1.5, 1.5, true);

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
