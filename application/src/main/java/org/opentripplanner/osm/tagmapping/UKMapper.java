package org.opentripplanner.osm.tagmapping;

import static org.opentripplanner.osm.wayproperty.MixinPropertiesBuilder.ofWalkSafety;
import static org.opentripplanner.osm.wayproperty.WayPropertiesBuilder.withModes;
import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;

import org.opentripplanner.osm.wayproperty.MixinPropertiesBuilder;
import org.opentripplanner.osm.wayproperty.WayProperties;
import org.opentripplanner.osm.wayproperty.WayPropertySet;
import org.opentripplanner.osm.wayproperty.specifier.LogicalOrSpecifier;

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
 */
class UKMapper extends OsmTagMapper {

  @Override
  public void populateProperties(WayPropertySet props) {
    props.setProperties("highway=cycleway", withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(0.6));
    props.setProperties("highway=bridleway", withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.3));

    // reduce trunk safety compared to default mapper
    props.setProperties("highway=trunk", withModes(ALL).walkSafety(2.5).bicycleSafety(2.5));
    props.setProperties("highway=trunk_link", withModes(ALL).walkSafety(2.5).bicycleSafety(2.06));

    // cancel out the effect of the reduced safety mixin for walking on trunk roads with pavement
    props.setMixinProperties(
      new LogicalOrSpecifier(
        "highway=trunk;sidewalk=yes",
        "highway=trunk;sidewalk=left",
        "highway=trunk;sidewalk=right",
        "highway=trunk;sidewalk=both",
        "highway=trunk_link;sidewalk=yes",
        "highway=trunk_link;sidewalk=left",
        "highway=trunk_link;sidewalk=right",
        "highway=trunk_link;sidewalk=both"
      ),
      ofWalkSafety(2)
    );

    props.setMixinProperties(
      "expressway=yes",
      MixinPropertiesBuilder.ofBicycleSafety(5).walkSafety(5)
    );

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

    WayProperties pedestrianWayProperties = withModes(PEDESTRIAN).build();
    props.setProperties("indoor=area", pedestrianWayProperties);
    props.setProperties("indoor=corridor", pedestrianWayProperties);

    super.populateProperties(props);
  }
}
