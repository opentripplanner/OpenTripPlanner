package org.opentripplanner.openstreetmap.tagmapping;

import static org.opentripplanner.openstreetmap.wayproperty.MixinPropertiesBuilder.ofBicycleSafety;
import static org.opentripplanner.openstreetmap.wayproperty.MixinPropertiesBuilder.ofWalkSafety;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.ExactMatchSpecifier.exact;

import org.opentripplanner.openstreetmap.wayproperty.WayPropertySet;
import org.opentripplanner.openstreetmap.wayproperty.specifier.Condition.Absent;
import org.opentripplanner.openstreetmap.wayproperty.specifier.Condition.GreaterThan;
import org.opentripplanner.openstreetmap.wayproperty.specifier.ExactMatchSpecifier;

class PortlandMapper implements OsmTagMapper {

  @Override
  public void populateProperties(WayPropertySet props) {
    props.setMixinProperties("footway=sidewalk", ofWalkSafety(1.1));
    props.setMixinProperties(new ExactMatchSpecifier(new Absent("name")), ofWalkSafety(1.2));
    props.setMixinProperties("highway=trunk", ofWalkSafety(1.2));
    props.setMixinProperties("highway=trunk_link", ofWalkSafety(1.2));
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

    /*
     * the RLIS/CCGIS:bicycle=designated mixins are coded out as they are no longer neccessary because of of the bicycle=designated block of code
     * above. This switch makes our weighting system less reliant on tags that aren't generally used by the OSM community, and prevents the double
     * counting that was occuring on streets with both bicycle infrastructure and an RLIS:bicycle=designated tag
     */

    /*
     * props.setProperties("RLIS:bicycle=designated", StreetTraversalPermission.ALL, 0.97, 0.97, true);
     */
    props.setMixinProperties("RLIS:bicycle=caution_area", ofBicycleSafety(1.45));
    props.setMixinProperties("RLIS:bicycle:right=caution_area", ofBicycleSafety(1.45, 1));
    props.setMixinProperties("RLIS:bicycle:left=caution_area", ofBicycleSafety(1, 1.45));
    /*
     * props.setProperties("CCGIS:bicycle=designated", StreetTraversalPermission.ALL, 0.97, 0.97, true);
     */
    props.setMixinProperties("CCGIS:bicycle=caution_area", ofBicycleSafety(1.45));
    props.setMixinProperties("CCGIS:bicycle:right=caution_area", ofBicycleSafety(1.45, 1));
    props.setMixinProperties("CCGIS:bicycle:left=caution_area", ofBicycleSafety(1, 1.45));


    // Max speed limit in Oregon is 70 mph ~= 113kmh ~= 31.3m/s
    props.maxPossibleCarSpeed = 31.4f;

    // Read the rest from the default set
    new DefaultMapper().populateProperties(props);
  }
}
