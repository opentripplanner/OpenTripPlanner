package org.opentripplanner.osm.tagmapping;

import static org.opentripplanner.osm.wayproperty.MixinPropertiesBuilder.ofBicycleSafety;
import static org.opentripplanner.osm.wayproperty.WayPropertiesBuilder.withModes;
import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;
import static org.opentripplanner.street.model.StreetTraversalPermission.BICYCLE_AND_CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;

import org.opentripplanner.osm.wayproperty.WayPropertySet;

/**
 * OSM way properties for German roads. Speed limits where adjusted to German regulation and some
 * bike safety settings tweaked, especially including tracktype's grade and preference of bicycle
 * networks.
 *
 * @see OsmTagMapper
 */
class GermanyMapper extends OsmTagMapper {

  @Override
  public void populateProperties(WayPropertySet props) {
    // Replace existing matching properties as the logic is that the first statement registered takes precedence over later statements

    // Automobile speeds in Germany. General speed limit is 50kph in settlements, 100kph outside settlements.
    // For motorways, there (currently still) is no limit. Nevertheless 120kph is assumed to reflect varying
    // traffic conditions.
    props.maxPossibleCarSpeed = 33.34f;
    props.setCarSpeed("highway=motorway", 33.33f); // = 120kph. Varies between 80 - 120 kph depending on road and season.
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
    props.setProperties("highway=track", withModes(PEDESTRIAN_AND_BICYCLE));
    props.setProperties("highway=track;surface=*", withModes(PEDESTRIAN_AND_BICYCLE));

    props.setProperties(
      "highway=residential;junction=roundabout",
      withModes(ALL).bicycleSafety(0.98)
    );
    props.setProperties("highway=*;junction=roundabout", withModes(BICYCLE_AND_CAR));

    // Pedestrian zones in Germany are forbidden for bicycles by default
    props.setProperties("highway=pedestrian", withModes(PEDESTRIAN));
    props.setProperties("highway=residential;maxspeed=30", withModes(ALL).bicycleSafety(0.9));
    props.setProperties(
      "highway=footway;bicycle=yes",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(0.8)
    );
    // Default was 2.5, we want to favor using mixed footways somewhat
    props.setMixinProperties("footway=sidewalk;highway=footway;bicycle=yes", ofBicycleSafety(0.6));

    props.setMixinProperties("highway=tertiary", ofBicycleSafety(1.2));
    props.setMixinProperties("maxspeed=70", ofBicycleSafety(1.5));
    props.setMixinProperties("maxspeed=80", ofBicycleSafety(2));
    props.setMixinProperties("maxspeed=90", ofBicycleSafety(3));
    props.setMixinProperties("maxspeed=100", ofBicycleSafety(5));

    // tracktypes

    // solid
    props.setMixinProperties("tracktype=grade1", ofBicycleSafety(1));
    // solid but unpaved
    props.setMixinProperties("tracktype=grade2", ofBicycleSafety(1.1));
    // mostly solid.
    props.setMixinProperties("tracktype=grade3", ofBicycleSafety(1.15));
    // mostly soft
    props.setMixinProperties("tracktype=grade4", ofBicycleSafety(1.3));
    // soft
    props.setMixinProperties("tracktype=grade5", ofBicycleSafety(1.5));

    // lit=yes currently is tagged very rarely, so we just want to discount where lit=no explicitly
    // not lit decreases safety
    props.setMixinProperties("lit=no", ofBicycleSafety(1.05));

    props.setProperties("highway=unclassified;cycleway=lane", withModes(ALL).bicycleSafety(0.87));

    super.populateProperties(props);
  }
}
