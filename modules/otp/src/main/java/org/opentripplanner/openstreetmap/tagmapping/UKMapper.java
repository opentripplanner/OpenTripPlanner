package org.opentripplanner.openstreetmap.tagmapping;

import static org.opentripplanner.openstreetmap.wayproperty.WayPropertiesBuilder.withModes;
import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;

import org.opentripplanner.openstreetmap.wayproperty.WayPropertySet;

/**
 * OSM way properties for UK roads. The main differences compared to the default property set are:
 * 1. In the UK there is no real distinction between trunk highways and primary highways, other than
 * the body responsible for them. Most highway=trunk and highway=trunk_link will allow traversal by
 * all modes. 2. Speeds have been set to reflect average free flow road speeds provided by UK DfT.
 * In particular note that a distinction is made between tertiary and unclassified/residential
 * roads. The default has these the same (25mph) but in the UK tertiary roads are considered by OSM
 * tagging guidelines to be busy unclassified through roads wide enough to allow two cars to pass
 * safely. The free flow speeds are therefore higher. These changes result in more realistic driving
 * routes. https://www.gov.uk/government/statistical-data-sets/vehicle-speed-compliance-statistics-data-tables-spe
 * https://wiki.openstreetmap.org/wiki/United_Kingdom_Tagging_Guidelines
 *
 * @author marcusyoung
 * @see OsmTagMapper
 * @see DefaultMapper
 */
class UKMapper implements OsmTagMapper {

  @Override
  public void populateProperties(WayPropertySet props) {
    // Replace existing matching properties as the logic is that the first statement registered takes precedence over later statements
    props.setProperties("highway=trunk_link", withModes(ALL).bicycleSafety(2.06));
    props.setProperties("highway=trunk", withModes(ALL).bicycleSafety(7.47));
    props.setProperties("highway=trunk;cycleway=lane", withModes(ALL).bicycleSafety(1.5));
    props.setProperties("highway=trunk_link;cycleway=lane", withModes(ALL).bicycleSafety(1.15));
    props.setProperties("highway=trunk;cycleway=share_busway", withModes(ALL).bicycleSafety(1.75));
    props.setProperties(
      "highway=trunk_link;cycleway=share_busway",
      withModes(ALL).bicycleSafety(1.25)
    );
    props.setProperties(
      "highway=trunk;cycleway=opposite_lane",
      withModes(ALL).bicycleSafety(7.47, 1.5)
    );
    props.setProperties(
      "highway=trunk_link;cycleway=opposite_lane",
      withModes(ALL).bicycleSafety(2.06, 1.15)
    );
    props.setProperties("highway=trunk;cycleway=track", withModes(ALL).bicycleSafety(0.95));
    props.setProperties("highway=trunk_link;cycleway=track", withModes(ALL).bicycleSafety(0.85));
    props.setProperties(
      "highway=trunk;cycleway=opposite_track",
      withModes(ALL).bicycleSafety(7.47, 0.95)
    );
    props.setProperties(
      "highway=trunk_link;cycleway=opposite_track",
      withModes(ALL).bicycleSafety(2.06, 0.85)
    );
    props.setProperties("highway=trunk;bicycle=designated", withModes(ALL).bicycleSafety(7.25));
    props.setProperties("highway=trunk_link;bicycle=designated", withModes(ALL).bicycleSafety(2));

    /*
     * Automobile speeds in UK. Based on recorded free flow speeds for motorways, trunk and primary and
     * my (marcusyoung) personal experience in obtaining realistic routes.
     *
     */
    // Max speed limit is 70 mph ~113kmh ~31.3m/s
    props.maxPossibleCarSpeed = 31.4f;
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
    new DefaultMapper().populateProperties(props);
  }
}
