package org.opentripplanner.graph_builder.module.osm;

import static org.opentripplanner.graph_builder.module.osm.WayPropertySetSource.DrivingDirection.RIGHT_HAND_TRAFFIC;

import org.opentripplanner.routing.core.intersection_model.IntersectionTraversalCostModel;
import org.opentripplanner.routing.core.intersection_model.NorwayIntersectionTraversalCostModel;
import org.opentripplanner.routing.core.intersection_model.SimpleIntersectionTraversalCostModel;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

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
        props.setCarSpeed(
                "highway=motorway",
                33.33f
        ); // = 120kph. Varies between 80 - 120 kph depending on road and season.
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
        props.setProperties(
                "highway=track", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.0, 1.0);
        props.setProperties(
                "highway=track;surface=*", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.0,
                1.0
        );

        props.setProperties(
                "highway=residential;junction=roundabout", StreetTraversalPermission.ALL, 0.98,
                0.98
        );
        props.setProperties(
                "highway=*;junction=roundabout", StreetTraversalPermission.BICYCLE_AND_CAR);

        // Pedestrian zones in Germany are forbidden for bicycles by default
        props.setProperties("highway=pedestrian", StreetTraversalPermission.PEDESTRIAN);
        props.setProperties(
                "highway=residential;maxspeed=30", StreetTraversalPermission.ALL, 0.9, 0.9);
        props.setProperties("highway=footway;bicycle=yes",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.8, 0.8
        );
        // Default was 2.5, we want to favor using mixed footways somewhat
        props.setProperties("footway=sidewalk;highway=footway;bicycle=yes",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.2, 1.2
        );

        props.setProperties("highway=tertiary", StreetTraversalPermission.ALL, 1.2, 1.2, true);
        props.setProperties("maxspeed=70", StreetTraversalPermission.ALL, 1.5, 1.5, true);
        props.setProperties("maxspeed=80", StreetTraversalPermission.ALL, 2.0, 2.0, true);
        props.setProperties("maxspeed=90", StreetTraversalPermission.ALL, 3.0, 3.0, true);
        props.setProperties("maxspeed=100", StreetTraversalPermission.ALL, 5.0, 5.0, true);

        // tracktypes

        // solid
        props.setProperties(
                "tracktype=grade1", StreetTraversalPermission.ALL, 1.0, 1.0, true);
        // solid but unpaved
        props.setProperties(
                "tracktype=grade2", StreetTraversalPermission.ALL, 1.1, 1.1,
                true
        );
        // mostly solid.
        props.setProperties(
                "tracktype=grade3", StreetTraversalPermission.ALL, 1.15, 1.15,
                true
        );
        // mostly soft
        props.setProperties(
                "tracktype=grade4", StreetTraversalPermission.ALL, 1.3, 1.3, true
        );
        // soft
        props.setProperties(
                "tracktype=grade5", StreetTraversalPermission.ALL, 1.5, 1.5, true
        );


        // lit=yes currently is tagged very rarely, so we just want to discount where lit=no explicitly
        // not lit decreases safety
        props.setProperties(
                "lit=no", StreetTraversalPermission.ALL, 1.05, 1.05,
                true
        );

        props.setProperties(
                "highway=unclassified;cycleway=lane", StreetTraversalPermission.ALL, 0.87, 0.87
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