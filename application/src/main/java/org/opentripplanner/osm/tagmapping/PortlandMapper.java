package org.opentripplanner.osm.tagmapping;

import static org.opentripplanner.osm.wayproperty.MixinPropertiesBuilder.ofBicycleSafety;
import static org.opentripplanner.osm.wayproperty.MixinPropertiesBuilder.ofWalkSafety;
import static org.opentripplanner.osm.wayproperty.specifier.Condition.Equals;
import static org.opentripplanner.osm.wayproperty.specifier.ExactMatchSpecifier.exact;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;

import org.opentripplanner.osm.wayproperty.WayProperties;
import org.opentripplanner.osm.wayproperty.WayPropertiesBuilder;
import org.opentripplanner.osm.wayproperty.WayPropertySet;
import org.opentripplanner.osm.wayproperty.specifier.Condition.Absent;
import org.opentripplanner.osm.wayproperty.specifier.Condition.GreaterThan;
import org.opentripplanner.osm.wayproperty.specifier.Condition.OneOf;
import org.opentripplanner.osm.wayproperty.specifier.ExactMatchSpecifier;

class PortlandMapper extends OsmTagMapper {

  private static final WayProperties SAFE_PEDESTRIAN = WayPropertiesBuilder.withModes(PEDESTRIAN)
    .walkSafety(1)
    .build();

  /**
   * Trunk is not here because it receives a very high default walk penalty.
   */
  private static final OneOf HIGHWAY_CONDITION = new OneOf(
    "highway",
    "primary",
    "primary_link",
    "secondary",
    "secondary_link",
    "tertiary",
    "tertiary_link",
    "residential",
    "unspecified"
  );

  @Override
  public WayPropertySet buildWayPropertySet() {
    var props = WayPropertySet.of();
    props.setProperties(oneOfHighway("footway", "sidewalk", "pedestrian"), SAFE_PEDESTRIAN);

    props.setMixinProperties(
      new ExactMatchSpecifier(HIGHWAY_CONDITION, new Absent("name")),
      ofWalkSafety(1.3)
    );
    props.setMixinProperties(
      new ExactMatchSpecifier(HIGHWAY_CONDITION, new Absent("sidewalk")),
      ofWalkSafety(1.2)
    );

    props.setMixinProperties(oneOfHighway("primary", "primary_link"), ofWalkSafety(1.8));
    props.setMixinProperties(oneOfHighway("secondary", "secondary_link"), ofWalkSafety(1.6));
    props.setMixinProperties(
      oneOfHighway("tertiary", "tertiary_link", "unclassified", "service"),
      ofWalkSafety(1.5)
    );
    props.setMixinProperties(oneOfHighway("residential"), ofWalkSafety(1.3));

    props.setMixinProperties(
      new ExactMatchSpecifier(new GreaterThan("lanes", 4)),
      ofWalkSafety(1.1)
    );
    props.setMixinProperties(highwaySidewalk("both"), ofWalkSafety(0.8));
    props.setMixinProperties(highwaySidewalk("left"), ofWalkSafety(0.9));
    props.setMixinProperties(highwaySidewalk("right"), ofWalkSafety(0.9));
    props.setMixinProperties(highwaySidewalk("separate"), ofWalkSafety(1.2));

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
    props.setMixinProperties("RLIS:bicycle:right=caution_area", ofBicycleSafety(1, 1.45, 1));
    props.setMixinProperties("RLIS:bicycle:left=caution_area", ofBicycleSafety(1, 1, 1.45));

    // https://taginfo.openstreetmap.org/keys/CCGIS:bicycle#overview
    props.setMixinProperties("CCGIS:bicycle=caution_area", ofBicycleSafety(1.45));
    props.setMixinProperties("CCGIS:bicycle:right=caution_area", ofBicycleSafety(1, 1.45, 1));
    props.setMixinProperties("CCGIS:bicycle:left=caution_area", ofBicycleSafety(1, 1, 1.45));

    // Max speed limit in Oregon is 70 mph ~= 113kmh ~= 31.3m/s
    props.setMaxPossibleCarSpeed(31.4f);

    return props.addPickers(super.buildWayPropertySet()).build();
  }

  private static ExactMatchSpecifier highwaySidewalk(String value) {
    return new ExactMatchSpecifier(HIGHWAY_CONDITION, new Equals("sidewalk", value));
  }

  private static ExactMatchSpecifier oneOfHighway(String... values) {
    return new ExactMatchSpecifier(new OneOf("highway", values));
  }
}
