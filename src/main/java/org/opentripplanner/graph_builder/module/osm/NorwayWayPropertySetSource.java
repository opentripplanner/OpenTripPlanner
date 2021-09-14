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

    // Do not drive on cycleWays
    props.setProperties("highway=cycleway;bicycle=designated",
        StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
        0.97,
        0.97
    );
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

    props.setProperties("highway=unclassified", StreetTraversalPermission.ALL, 1.0, 1.0);
    props.setProperties("highway=unclassified;foot=no", StreetTraversalPermission.BICYCLE_AND_CAR, 6, 6);
    // These access tags indicates low traffic
    props.setProperties("highway=unclassified;motor_vehicle=no", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.97, 0.97);
    props.setProperties("highway=unclassified;motor_vehicle=private", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.97, 0.97);
    props.setProperties("highway=unclassified;motor_vehicle=permit", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.97, 0.97);
    props.setProperties("highway=unclassified;motor_vehicle=destination", StreetTraversalPermission.ALL, 0.97, 0.97);
    props.setProperties("highway=unclassified;maxspeed=70", StreetTraversalPermission.ALL, 2.06, 2.06);
    props.setProperties("highway=unclassified;maxspeed=80", StreetTraversalPermission.ALL, 2.06, 2.06);

    props.setProperties("highway=residential", StreetTraversalPermission.ALL, 1.0, 1.0);
    // These access tags indicates low traffic
    props.setProperties("highway=residential;motor_vehicle=no", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.97, 0.97);
    props.setProperties("highway=residential;motor_vehicle=private", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.97, 0.97);
    props.setProperties("highway=residential;motor_vehicle=permit", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.97, 0.97);
    props.setProperties("highway=residential;motor_vehicle=destination", StreetTraversalPermission.ALL, 0.97, 0.97);

    props.setProperties("highway=service", StreetTraversalPermission.ALL, 1.0, 1.0);
    props.setProperties("highway=service;foot=no", StreetTraversalPermission.BICYCLE_AND_CAR, 6, 6);
    // These access tags indicates low traffic
    props.setProperties("highway=service;motor_vehicle=no", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.97, 0.97);
    props.setProperties("highway=service;motor_vehicle=private", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.97, 0.97);
    props.setProperties("highway=service;motor_vehicle=permit", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.97, 0.97);
    props.setProperties("highway=service;motor_vehicle=destination", StreetTraversalPermission.ALL, 0.97, 0.97);
    // Cycling around reversing cars on a parking lot feels unsafe
    props.setProperties("highway=service;service=parking_aisle", StreetTraversalPermission.ALL, 1.5, 1.5);
    props.setProperties("highway=service;service=drive-through", StreetTraversalPermission.ALL, 1.5, 1.5);

    // Disallow paths that are not fit for walking
    props.setProperties("smoothness=horrible", StreetTraversalPermission.NONE);
    props.setProperties("smoothness=impassable", StreetTraversalPermission.NONE);
    props.setProperties("smoothness=very_horrible", StreetTraversalPermission.NONE);
    props.setProperties("smoothness=very_bad", StreetTraversalPermission.NONE);
    props.setProperties("smoothness=bad", StreetTraversalPermission.NONE);
    props.setProperties("smoothness=rough", StreetTraversalPermission.NONE);

    props.setProperties("trail_visibility=bad", StreetTraversalPermission.NONE);
    props.setProperties("trail_visibility=no", StreetTraversalPermission.NONE);
    props.setProperties("trail_visibility=low", StreetTraversalPermission.NONE);
    props.setProperties("trail_visibility=poor", StreetTraversalPermission.NONE);

    props.setProperties("sac_scale=mountain_hiking", StreetTraversalPermission.NONE);
    props.setProperties("sac_scale=demanding_mountain_hiking", StreetTraversalPermission.NONE);
    props.setProperties("sac_scale=alpine_hiking", StreetTraversalPermission.NONE);
    props.setProperties("sac_scale=demanding_alpine_hiking", StreetTraversalPermission.NONE);
    props.setProperties("sac_scale=difficult_alpine_hiking", StreetTraversalPermission.NONE);

    // Allow bikes on sidewalks
    props.setProperties(
        "footway=sidewalk;highway=footway",
        StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE
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
