package org.opentripplanner.osm.tagmapping;

import static org.opentripplanner.osm.wayproperty.MixinPropertiesBuilder.ofBicycleSafety;
import static org.opentripplanner.osm.wayproperty.MixinPropertiesBuilder.ofWalkSafety;
import static org.opentripplanner.osm.wayproperty.specifier.ExactMatchSpecifier.exact;

import org.opentripplanner.osm.wayproperty.WayPropertySet;
import org.opentripplanner.osm.wayproperty.specifier.Condition.Absent;
import org.opentripplanner.osm.wayproperty.specifier.Condition.GreaterThan;
import org.opentripplanner.osm.wayproperty.specifier.ExactMatchSpecifier;

class PortlandMapper extends OsmTagMapper {

  @Override
  public void populateProperties(WayPropertySet props) {
    props.setMixinProperties("footway=sidewalk", ofWalkSafety(1.1));
    props.setMixinProperties(new ExactMatchSpecifier(new Absent("name")), ofWalkSafety(1.2));
    props.setMixinProperties("highway=trunk", ofWalkSafety(1.2 / 7.47));
    props.setMixinProperties("highway=trunk_link", ofWalkSafety(1.2 / 7.47));
    props.setMixinProperties("highway=primary", ofWalkSafety(1.2));
    props.setMixinProperties("highway=primary_link", ofWalkSafety(1.2));
    props.setMixinProperties("highway=secondary", ofWalkSafety(1.1));
    props.setMixinProperties("highway=secondary_link", ofWalkSafety(1.1));
    props.setMixinProperties("highway=tertiary", ofWalkSafety(1.1));
    props.setMixinProperties("highway=tertiary_link", ofWalkSafety(1.1));
    props.setMixinProperties(
      new ExactMatchSpecifier(new GreaterThan("lanes", 4)),
      ofWalkSafety(1.1)
    );
    props.setMixinProperties("sidewalk=both", ofWalkSafety(0.8));
    props.setMixinProperties("sidewalk=left", ofWalkSafety(0.9));
    props.setMixinProperties("sidewalk=right", ofWalkSafety(0.9));
    props.setMixinProperties("surface=unpaved", ofWalkSafety(1.4));
    // high penalty for streets with no sidewalk
    // these are using the exact() call to generate a ExactMatch. without it several of these
    // would apply to the same way that is tagged with sidewalk=no and compounding the safety to a very
    // high value as they are all multiplied with each other.
    props.setMixinProperties(exact("sidewalk=no;maxspeed=55 mph"), ofWalkSafety(6));
    props.setMixinProperties(exact("sidewalk=no;maxspeed=50 mph"), ofWalkSafety(5));
    props.setMixinProperties(exact("sidewalk=no;maxspeed=45 mph"), ofWalkSafety(4));
    props.setMixinProperties(exact("sidewalk=no;maxspeed=40 mph"), ofWalkSafety(3));
    props.setMixinProperties(exact("sidewalk=no;maxspeed=35 mph"), ofWalkSafety(2));
    props.setMixinProperties(exact("sidewalk=no;maxspeed=30 mph"), ofWalkSafety(1.5));

    // rarely used tags that are specific to counties near Portland
    // https://taginfo.openstreetmap.org/keys/RLIS:bicycle#overview

    props.setMixinProperties("RLIS:bicycle=caution_area", ofBicycleSafety(1.45));
    props.setMixinProperties("RLIS:bicycle:right=caution_area", ofBicycleSafety(1.45, 1));
    props.setMixinProperties("RLIS:bicycle:left=caution_area", ofBicycleSafety(1, 1.45));

    // https://taginfo.openstreetmap.org/keys/CCGIS:bicycle#overview
    props.setMixinProperties("CCGIS:bicycle=caution_area", ofBicycleSafety(1.45));
    props.setMixinProperties("CCGIS:bicycle:right=caution_area", ofBicycleSafety(1.45, 1));
    props.setMixinProperties("CCGIS:bicycle:left=caution_area", ofBicycleSafety(1, 1.45));

    // Max speed limit in Oregon is 70 mph ~= 113kmh ~= 31.3m/s
    props.maxPossibleCarSpeed = 31.4f;

    super.populateProperties(props);
  }
}
