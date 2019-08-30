package org.opentripplanner.graph_builder.module.osm;

import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

/**
 * OSM way properties for UK roads. 
 * The main differences compared to the default property set are:
 * 1. In the UK there is no real distinction between trunk highways and primary highways, other than the
 * body responsible for them. Most highway=trunk and highway=trunk_link will allow traversal by all modes.
 * 2. Speeds have been set to reflect average free flow road speeds provided by UK DfT. In particular
 * note that a distinction is made between tertiary and unclassified/residential roads. The default has these
 * the same (25mph) but in the UK tertiary roads are considered by OSM tagging guidelines to be busy unclassified
 * through roads wide enough to allow two cars to pass safely. The free flow speeds are therefore higher. 
 * These changes result in more realistic driving routes.
 * https://www.gov.uk/government/statistical-data-sets/vehicle-speed-compliance-statistics-data-tables-spe
 * https://wiki.openstreetmap.org/wiki/United_Kingdom_Tagging_Guidelines
 *   
 * @author marcusyoung
 * @see WayPropertySetSource
 * @see DefaultWayPropertySetSource
 */
public class UKWayPropertySetSource implements WayPropertySetSource {

    @Override
    public void populateProperties(WayPropertySet props) {
        // Replace existing matching properties as the logic is that the first statement registered takes precedence over later statements
        props.setProperties("highway=trunk_link", StreetTraversalPermission.ALL, 2.06, 2.06);
        props.setProperties("highway=trunk", StreetTraversalPermission.ALL, 7.47, 7.47);
        props.setProperties("highway=trunk;cycleway=lane", StreetTraversalPermission.ALL, 1.5,
                1.5);
        props.setProperties("highway=trunk_link;cycleway=lane", StreetTraversalPermission.ALL,
                1.15, 1.15);
        props.setProperties("highway=trunk;cycleway=share_busway", StreetTraversalPermission.ALL,
                1.75, 1.75);
        props.setProperties("highway=trunk_link;cycleway=share_busway",
                StreetTraversalPermission.ALL,1.25, 1.25);
        props.setProperties("highway=trunk;cycleway=opposite_lane", StreetTraversalPermission.ALL,
                7.47, 1.5);
        props.setProperties("highway=trunk_link;cycleway=opposite_lane",
                StreetTraversalPermission.ALL, 2.06, 1.15);
        props.setProperties("highway=trunk;cycleway=track", StreetTraversalPermission.ALL, 0.95,
                0.95);
        props.setProperties("highway=trunk_link;cycleway=track", StreetTraversalPermission.ALL,
                0.85, 0.85);
        props.setProperties("highway=trunk;cycleway=opposite_track", StreetTraversalPermission.ALL,
                7.47, 0.95);
        props.setProperties("highway=trunk_link;cycleway=opposite_track",
                StreetTraversalPermission.ALL, 2.06, 0.85);
        props.setProperties("highway=trunk;bicycle=designated", StreetTraversalPermission.ALL,
                7.25, 7.25);
        props.setProperties("highway=trunk_link;bicycle=designated", StreetTraversalPermission.ALL,
                2, 2);

        /*
         * Automobile speeds in UK. Based on recorded free flow speeds for motorways, trunk and primary and 
         * my (marcusyoung) personal experience in obtaining realistic routes.
         * 
         */
        props.setCarSpeed("highway=motorway", 30.4f); // ~=68mph
        props.setCarSpeed("highway=motorway_link", 22.4f); // ~= 50mph
        props.setCarSpeed("highway=trunk", 22.4f); // ~=50mph
        props.setCarSpeed("highway=trunk_link", 17.9f); // ~= 40mph
        props.setCarSpeed("highway=primary", 22.4f); // ~=50mph
        props.setCarSpeed("highway=primary_link", 17.9f); // ~= 40mph
        props.setCarSpeed("highway=secondary", 17.9f); // ~= 40mph
        props.setCarSpeed("highway=secondary_link", 13.4f); // ~= 30mph
        props.setCarSpeed("highway=tertiary", 15.7f); // ~= 35mph

        // Read the rest from the default set
        new DefaultWayPropertySetSource().populateProperties(props);
    }
}
