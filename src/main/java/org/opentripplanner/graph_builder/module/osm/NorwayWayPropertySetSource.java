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
    // Replace existing matching properties as the logic is that the first statement registered takes precedence over later statements
    props.setProperties("highway=trunk_link", StreetTraversalPermission.ALL, 2.06, 2.06);
    props.setProperties("highway=trunk", StreetTraversalPermission.ALL, 7.47, 7.47);

    // Don't recommend walking in trunk road tunnels (although actually legal unless explicitly forbidden)
    props.setProperties("highway=trunk;tunnel=yes", StreetTraversalPermission.CAR, 7.47, 7.47);
    props.setProperties("highway=trunk_link;tunnel=yes", StreetTraversalPermission.CAR, 7.47, 7.47);

    // Various highway tags that should not be routed on
    props.setProperties("highway=razed", StreetTraversalPermission.NONE);
    props.setProperties("highway=proposed", StreetTraversalPermission.NONE);
    props.setProperties("highway=construction", StreetTraversalPermission.NONE);
    props.setProperties("highway=abandoned", StreetTraversalPermission.NONE);
    props.setProperties("highway=winter_road", StreetTraversalPermission.NONE);
    props.setProperties("highway=historic", StreetTraversalPermission.NONE);
    props.setProperties("highway=proposal_alternative", StreetTraversalPermission.NONE);

    // Do not walk on "Motortrafikkvei" ("motorvei klasse b")
    props.setProperties("motorroad=yes", StreetTraversalPermission.CAR, 7.47, 7.47);

    // Do not drive on cycleWays
    props.setProperties("highway=cycleway;bicycle=designated",
        StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
        0.97,
        0.97
    );

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

    // Read the rest from the default set
    new DefaultWayPropertySetSource().populateProperties(props);
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
