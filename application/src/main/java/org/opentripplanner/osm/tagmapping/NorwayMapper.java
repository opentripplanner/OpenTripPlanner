package org.opentripplanner.osm.tagmapping;

import static org.opentripplanner.osm.wayproperty.MixinPropertiesBuilder.ofBicycleSafety;
import static org.opentripplanner.osm.wayproperty.MixinPropertiesBuilder.ofWalkSafety;
import static org.opentripplanner.osm.wayproperty.WayPropertiesBuilder.withModes;
import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;
import static org.opentripplanner.street.model.StreetTraversalPermission.CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.NONE;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;

import java.util.function.BiFunction;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.wayproperty.WayPropertySet;
import org.opentripplanner.osm.wayproperty.specifier.BestMatchSpecifier;
import org.opentripplanner.osm.wayproperty.specifier.Condition;
import org.opentripplanner.osm.wayproperty.specifier.ExactMatchSpecifier;
import org.opentripplanner.osm.wayproperty.specifier.LogicalOrSpecifier;

/**
 * OSM way properties for Norwegian roads. The main difference compared to the default property set
 * is that most of the highway=trunk roads also allows walking and biking, where as some does not.
 * http://wiki.openstreetmap.org/wiki/Tag:highway%3Dtrunk http://wiki.openstreetmap.org/wiki/Highway:International_equivalence
 *
 * @author seime
 * @see OsmTagMapper
 * @see OsmTagMapper
 */
class NorwayMapper extends OsmTagMapper {

  @Override
  public void populateProperties(WayPropertySet props) {
    var hasSidewalk = new Condition.OneOf("sidewalk", "yes", "left", "right", "both");
    var hasPrefixSidewalk = new Condition.Equals("sidewalk", "yes"); // e.g sidewalk:left=yes
    props.setDefaultWalkSafetyForPermission((permission, speedLimit, way) ->
      switch (permission) {
        case ALL, PEDESTRIAN_AND_CAR -> {
          if (
            hasSidewalk.isMatch(way) ||
            hasPrefixSidewalk.isLeftMatch(way) ||
            hasPrefixSidewalk.isRightMatch(way)
          ) {
            yield 1.1;
          }
          // 90 km/h or over
          else if (speedLimit >= 25f) {
            yield 3.;
          }
          // ~60 km/h or over
          else if (speedLimit >= 16.6f) {
            yield 1.9;
          }
          // ~40 km/h or over
          else if (speedLimit >= 11.1f) {
            yield 1.6;
          }
          // 30 km/h or lower
          else {
            yield 1.45;
          }
        }
        case PEDESTRIAN_AND_BICYCLE -> 1.15;
        case PEDESTRIAN -> 1.;
        // these don't include walking
        case BICYCLE_AND_CAR, BICYCLE, CAR, NONE -> 3.;
      }
    );

    var cycleSafetyVeryHighTraffic = 10.;
    var cycleSafetyHighTraffic = 3.75;
    var cycleSafetyMediumHighTraffic = 3.43;
    var cycleSafetyMediumTraffic = 2.5;
    var cycleSafetyMediumLowTraffic = 2.37;
    var cycleSafetyLowTraffic = 1.83;
    var cycleSafetyVeryLowTraffic = 1.57;

    var isTrunkOrPrimary = new Condition.OneOf(
      "highway",
      "trunk",
      "trunk_link",
      "primary",
      "primary_link"
    );
    var isSecondaryHighway = new Condition.OneOf("highway", "secondary", "secondary_link");
    var isTertiaryHighway = new Condition.OneOf("highway", "tertiary", "tertiary_link");
    var isClassifiedRoad = new Condition.OneOf(
      "highway",
      "trunk",
      "trunk_link",
      "primary",
      "primary_link",
      "secondary",
      "secondary_link",
      "tertiary",
      "tertiary_link"
    );
    var isClassifiedOrUnclassifiedRoad = new Condition.OneOf(
      "highway",
      "trunk",
      "trunk_link",
      "primary",
      "primary_link",
      "secondary",
      "secondary_link",
      "tertiary",
      "tertiary_link",
      "unclassified"
    );

    var isNormalRoad = new Condition.OneOf(
      "highway",
      "trunk",
      "trunk_link",
      "primary",
      "primary_link",
      "secondary",
      "secondary_link",
      "tertiary",
      "tertiary_link",
      "unclassified",
      "residential"
    );

    BiFunction<Float, OsmEntity, Double> cycleSafetyHighway = (speedLimit, way) -> {
      if (way.isPedestrianDenied()) {
        return cycleSafetyVeryHighTraffic;
      }
      // 90 km/h or over
      else if (speedLimit >= 25f) {
        return cycleSafetyVeryHighTraffic;
      }
      // ~70 km/h or over
      else if (speedLimit >= 19.4f) {
        if (isTrunkOrPrimary.isMatch(way)) {
          return cycleSafetyHighTraffic;
        } else return cycleSafetyMediumHighTraffic;
      }
      // between ~60 km/h and ~40 km/
      else if (speedLimit >= 11.1f) {
        if (isTrunkOrPrimary.isMatch(way)) {
          // 60 km/h or 50 to 40 km/h
          return speedLimit >= 16.6f ? cycleSafetyMediumHighTraffic : cycleSafetyMediumTraffic;
        } else if (isSecondaryHighway.isMatch(way)) {
          // ~60 km/h or 50 to 40 km/h
          return speedLimit >= 16.6f ? cycleSafetyMediumTraffic : cycleSafetyMediumLowTraffic;
        } else if (isTertiaryHighway.isMatch(way)) {
          // ~60 to 50 km/h or 40 km/h
          return speedLimit >= 13.8f ? cycleSafetyMediumLowTraffic : cycleSafetyLowTraffic;
        }
      }
      // 30 km/h or lower, or lower road class than unclassified
      if (
        this.isMotorVehicleThroughTrafficExplicitlyDisallowed(way)
      ) return cycleSafetyVeryLowTraffic;
      else return cycleSafetyLowTraffic;
    };

    props.setDefaultBicycleSafetyForPermission((permission, speedLimit, way) ->
      switch (permission) {
        case ALL -> cycleSafetyHighway.apply(speedLimit, way);
        case BICYCLE_AND_CAR -> cycleSafetyVeryHighTraffic;
        case PEDESTRIAN_AND_BICYCLE -> 1.12;
        case BICYCLE -> 1.05;
        // these don't include cycling
        case PEDESTRIAN_AND_CAR, PEDESTRIAN, CAR, NONE -> cycleSafetyVeryHighTraffic;
      }
    );

    props.setProperties(
      new ExactMatchSpecifier(new Condition.OneOf("highway", "motorway", "motorway_link")),
      withModes(CAR)
    );

    // Walking and cycling illegal on "Motortrafikkvei"
    props.setProperties(
      new ExactMatchSpecifier(isTrunkOrPrimary, new Condition.Equals("motorroad", "yes")),
      withModes(CAR)
    );

    props.setProperties(new ExactMatchSpecifier(isNormalRoad), withModes(ALL));

    /* bicycle infrastructure */

    var cycleLaneInHighTraffic = withModes(ALL).bicycleSafety(1.27).build();
    var cycleLaneInLowTraffic = withModes(ALL).bicycleSafety(1.1).build();

    props.setProperties(
      new ExactMatchSpecifier(new Condition.Equals("cycleway", "track"), isNormalRoad),
      withModes(ALL).bicycleSafety(1).build()
    );

    props.setProperties(
      new ExactMatchSpecifier(new Condition.Equals("cycleway", "lane"), isClassifiedRoad),
      cycleLaneInHighTraffic
    );

    props.setProperties(
      new ExactMatchSpecifier(
        new Condition.Equals("cycleway", "lane"),
        new Condition.LessThan("maxspeed", 50),
        isClassifiedRoad
      ),
      cycleLaneInLowTraffic
    );

    props.setProperties(
      new ExactMatchSpecifier(
        new Condition.Equals("cycleway", "lane"),
        new Condition.OneOf("highway", "unclassified", "residential")
      ),
      cycleLaneInLowTraffic
    );

    props.setMixinProperties(
      new ExactMatchSpecifier(new Condition.Equals("cycleway", "shared_lane"), isNormalRoad),
      ofBicycleSafety(0.85)
    );

    //relation properties are copied over to ways
    props.setMixinProperties(
      new LogicalOrSpecifier("lcn=yes", "rcn=yes", "ncn=yes"),
      ofBicycleSafety(0.85)
    );

    props.setMixinProperties(
      new ExactMatchSpecifier(
        new Condition.Equals("oneway", "yes"),
        new Condition.OneOfOrAbsent("cycleway"),
        isNormalRoad
      ),
      ofBicycleSafety(1, 1, 1.15)
    );

    // Discourage cycling along tram tracks
    props.setMixinProperties(
      new ExactMatchSpecifier(
        new Condition.OneOf("embedded_rails", "tram", "light_rail", "disused")
      ),
      ofBicycleSafety(1.2)
    );

    // Discourage cycling and walking in road tunnels
    props.setMixinProperties(
      new ExactMatchSpecifier(
        new Condition.Equals("tunnel", "yes"),
        isClassifiedOrUnclassifiedRoad
      ),
      ofBicycleSafety(2).walkSafety(2)
    );

    // Discourage walking on a bridge and other ways without a verge without a sidewalk
    props.setMixinProperties(
      new LogicalOrSpecifier(
        new ExactMatchSpecifier(
          new Condition.Equals("bridge", "yes"),
          new Condition.OneOfOrAbsent("sidewalk", "no", "separate"),
          isClassifiedOrUnclassifiedRoad
        ),
        new ExactMatchSpecifier(
          new Condition.Equals("verge", "no"),
          new Condition.OneOfOrAbsent("sidewalk", "no", "separate"),
          isClassifiedOrUnclassifiedRoad
        )
      ),
      ofWalkSafety(2.)
    );

    // Discourage walking in roundabouts
    props.setMixinProperties(
      new ExactMatchSpecifier(
        new Condition.Equals("junction", "roundabout"),
        new Condition.OneOfOrAbsent("sidewalk", "no", "separate")
      ),
      ofWalkSafety(2.)
    );

    props.setProperties("highway=service", withModes(ALL));
    // Cycling around reversing cars on a parking lot feels unsafe
    props.setProperties(
      "highway=service;service=parking_aisle",
      withModes(ALL).bicycleSafety(cycleSafetyMediumTraffic)
    );
    props.setProperties(
      "highway=service;service=drive-through",
      withModes(ALL).bicycleSafety(cycleSafetyMediumTraffic)
    );

    /* Pedestrian, living street and busway */
    props.setProperties(
      "highway=living_street",
      withModes(ALL).bicycleSafety(cycleSafetyLowTraffic)
    );
    props.setProperties("highway=pedestrian", withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.2));
    props.setProperties(
      "highway=busway",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(cycleSafetyMediumLowTraffic).walkSafety(1.9)
    );
    props.setProperties(
      new ExactMatchSpecifier(
        new Condition.Equals("highway", "service"),
        new Condition.OneOf("bus", "yes", "designated")
      ),
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(cycleSafetyMediumLowTraffic).walkSafety(1.9)
    );

    /* Footway and cycleway */
    var footway = withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.42).walkSafety(1.).build();
    var cycleway = withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.05).walkSafety(1.4).build();
    var cyclewayWithSidewalk = withModes(PEDESTRIAN_AND_BICYCLE)
      .bicycleSafety(1.05)
      .walkSafety(1.)
      .build();
    var twoLaneOrOnewayCycleway = withModes(PEDESTRIAN_AND_BICYCLE)
      .bicycleSafety(1.)
      .walkSafety(1.4)
      .build();
    var twoLaneOrOnewayCyclewayWithSidewalk = withModes(PEDESTRIAN_AND_BICYCLE)
      .bicycleSafety(1.)
      .walkSafety(1.)
      .build();
    var combinedFootAndCycleway = withModes(PEDESTRIAN_AND_BICYCLE)
      .bicycleSafety(1.05)
      .walkSafety(1.15)
      .build();
    props.setProperties("highway=footway", footway);

    props.setProperties("highway=cycleway", cycleway);
    props.setProperties(
      new ExactMatchSpecifier(
        new Condition.Equals("highway", "cycleway"),
        new Condition.GreaterThan("lanes", 1)
      ),
      twoLaneOrOnewayCycleway
    );
    props.setProperties("highway=cycleway;oneway=yes", twoLaneOrOnewayCycleway);
    props.setProperties(
      new ExactMatchSpecifier(new Condition.Equals("highway", "cycleway"), hasSidewalk),
      cyclewayWithSidewalk
    );
    props.setProperties(
      new ExactMatchSpecifier(
        new Condition.Equals("highway", "cycleway"),
        new Condition.GreaterThan("lanes", 1),
        hasSidewalk
      ),
      twoLaneOrOnewayCyclewayWithSidewalk
    );
    props.setProperties(
      new ExactMatchSpecifier(
        new Condition.Equals("highway", "cycleway"),
        new Condition.Equals("oneway", "yes"),
        hasSidewalk
      ),
      twoLaneOrOnewayCyclewayWithSidewalk
    );
    props.setProperties("highway=cycleway;foot=designated;segregated=no", combinedFootAndCycleway);
    props.setProperties(
      "highway=path;foot=designated;bicycle=designated;segregated=no",
      combinedFootAndCycleway
    );
    props.setProperties("highway=cycleway;foot=designated;segregated=yes", cyclewayWithSidewalk);
    props.setProperties(
      "highway=path;foot=designated;bicycle=designated;segregated=yes",
      cyclewayWithSidewalk
    );
    props.setProperties(
      new ExactMatchSpecifier(
        new Condition.Equals("highway", "cycleway"),
        new Condition.Equals("foot", "designated"),
        new Condition.Equals("segregated", "yes"),
        new Condition.GreaterThan("lanes", 1)
      ),
      twoLaneOrOnewayCyclewayWithSidewalk
    );
    // "motor_vehicle=destination" indicates unwanted car traffic, signposted "Kjøring til eiendommene tillatt"
    props.setProperties(
      "highway=cycleway;foot=designated;segregated=*;motor_vehicle=destination",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(cycleSafetyVeryLowTraffic)
    );
    props.setProperties(
      "highway=path;foot=designated;bicycle=designated;segregated=*;motor_vehicle=destination",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(cycleSafetyVeryLowTraffic)
    );
    props.setProperties(
      "highway=footway;footway=sidewalk",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.93).walkSafety(1.1)
    );
    props.setProperties(
      "highway=footway;footway=crossing",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(2.33).walkSafety(1.35)
    );
    props.setProperties(
      "highway=cycleway;cycleway=crossing",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(2.33).walkSafety(1.35)
    );

    props.setProperties("highway=track", withModes(PEDESTRIAN_AND_BICYCLE));
    props.setProperties("highway=bridleway", withModes(PEDESTRIAN_AND_BICYCLE));
    props.setProperties("highway=path", withModes(PEDESTRIAN_AND_BICYCLE));
    props.setProperties("highway=steps", withModes(PEDESTRIAN));
    props.setProperties("highway=corridor", withModes(PEDESTRIAN));
    props.setProperties("highway=footway;indoor=yes", withModes(PEDESTRIAN));
    props.setProperties("highway=platform", withModes(PEDESTRIAN));
    props.setProperties("public_transport=platform", withModes(PEDESTRIAN));

    props.setProperties(
      new ExactMatchSpecifier(
        new Condition.OneOf("trail_visibility", "bad", "low", "poor", "horrible", "no"),
        new Condition.Equals("highway", "path")
      ),
      withModes(NONE)
    );
    props.setProperties(
      new ExactMatchSpecifier(
        new Condition.OneOf(
          "sac_scale",
          "demanding_mountain_hiking",
          "alpine_hiking",
          "demanding_alpine_hiking",
          "difficult_alpine_hiking"
        ),
        new Condition.OneOf("highway", "path", "steps")
      ),
      withModes(NONE)
    );
    props.setProperties(
      new ExactMatchSpecifier(
        new Condition.OneOf("smoothness", "horrible", "very_horrible"),
        new Condition.OneOf("highway", "path", "bridleway", "track")
      ),
      withModes(PEDESTRIAN).walkSafety(1.15)
    );
    props.setProperties(
      new ExactMatchSpecifier(
        new Condition.Equals("smoothness", "impassable"),
        new Condition.OneOf("highway", "path", "bridleway", "track")
      ),
      withModes(NONE)
    );
    props.setProperties(
      new ExactMatchSpecifier(
        new Condition.InclusiveRange("mtb:scale", 2, 1),
        new Condition.OneOf("highway", "path", "bridleway", "track")
      ),
      withModes(PEDESTRIAN).walkSafety(1.15)
    );
    props.setProperties(
      new ExactMatchSpecifier(
        new Condition.GreaterThan("mtb:scale", 2),
        new Condition.OneOf("highway", "path", "bridleway", "track")
      ),
      withModes(NONE)
    );

    // Paved but unfavorable for bicycles
    props.setMixinProperties("surface=grass_paver", ofBicycleSafety(1.2));

    props.setMixinProperties("surface=sett", ofBicycleSafety(1.2));
    props.setMixinProperties("surface=cobblestone", ofBicycleSafety(1.2));
    props.setMixinProperties("surface=unhewn_cobblestone", ofBicycleSafety(3.));
    // Slick if wet
    props.setMixinProperties("surface=metal_grid", ofBicycleSafety(1.2));
    props.setMixinProperties("surface=metal", ofBicycleSafety(1.2));
    // Paved but damaged
    var isPaved = new Condition.OneOf(
      "surface",
      "asfalt",
      "concrete",
      "paving_stones",
      "paved",
      "wood"
    );

    props.setMixinProperties(
      new ExactMatchSpecifier(new Condition.Equals("smoothness", "intermediate"), isPaved),
      ofBicycleSafety(1.2)
    );

    props.setMixinProperties(
      new ExactMatchSpecifier(new Condition.Equals("smoothness", "bad"), isPaved),
      ofBicycleSafety(1.4).walkSafety(1.6)
    );
    // Unpaved
    props.setMixinProperties(
      new ExactMatchSpecifier(
        new Condition.Equals("surface", "unpaved"),
        new Condition.Absent("tracktype")
      ),
      ofBicycleSafety(1.8).walkSafety(1.6)
    );
    props.setMixinProperties("surface=compacted", ofBicycleSafety(1.4).walkSafety(1.4));
    props.setMixinProperties("surface=fine_gravel", ofBicycleSafety(1.8).walkSafety(1.6));
    props.setMixinProperties("surface=pebblestone", ofBicycleSafety(1.8).walkSafety(1.6));
    props.setMixinProperties("surface=gravel", ofBicycleSafety(1.8).walkSafety(1.6));
    props.setMixinProperties("surface=woodchip", ofBicycleSafety(1.8).walkSafety(1.6));
    props.setMixinProperties("surface=ground", ofBicycleSafety(2.3).walkSafety(2.4));
    props.setMixinProperties("surface=dirt", ofBicycleSafety(2.3).walkSafety(2.4));
    props.setMixinProperties("surface=earth", ofBicycleSafety(2.3).walkSafety(2.4));
    props.setMixinProperties("surface=grass", ofBicycleSafety(2.3).walkSafety(1.8));
    props.setMixinProperties("surface=mud", ofBicycleSafety(3.).walkSafety(3.));
    props.setMixinProperties("surface=sand", ofBicycleSafety(3.).walkSafety(1.8));

    props.setMixinProperties(
      new ExactMatchSpecifier(
        new Condition.Absent("tracktype"),
        new Condition.OneOfOrAbsent("surface", "unpaved"),
        new Condition.OneOf("highway", "track", "bridleway")
      ),
      ofBicycleSafety(1.8).walkSafety(1.6)
    );
    props.setMixinProperties(
      new ExactMatchSpecifier(
        new Condition.Equals("tracktype", "grade2"),
        new Condition.OneOfOrAbsent("surface", "unpaved"),
        new Condition.OneOf("highway", "track", "bridleway", "service", "unclassified")
      ),
      ofBicycleSafety(1.4).walkSafety(1.4)
    );
    props.setMixinProperties(
      new ExactMatchSpecifier(
        new Condition.Equals("tracktype", "grade3"),
        new Condition.OneOfOrAbsent("surface", "unpaved"),
        new Condition.OneOf("highway", "track", "bridleway", "service", "unclassified")
      ),
      ofBicycleSafety(1.8).walkSafety(1.6)
    );
    props.setMixinProperties(
      new ExactMatchSpecifier(
        new Condition.Equals("tracktype", "grade4"),
        new Condition.OneOfOrAbsent("surface", "unpaved"),
        new Condition.OneOf("highway", "track", "bridleway", "service", "unclassified")
      ),
      ofBicycleSafety(2.3).walkSafety(1.8)
    );
    props.setMixinProperties(
      new ExactMatchSpecifier(
        new Condition.Equals("tracktype", "grade5"),
        new Condition.OneOfOrAbsent("surface", "unpaved"),
        new Condition.OneOf("highway", "track", "bridleway", "service", "unclassified")
      ),
      ofBicycleSafety(2.3).walkSafety(2.4)
    );
    props.setMixinProperties(
      new ExactMatchSpecifier(
        new Condition.OneOfOrAbsent("surface"),
        new Condition.Equals("highway", "path")
      ),
      ofBicycleSafety(2.3).walkSafety(2.4)
    );

    props.setMixinProperties("sac_scale=mountain_hiking", ofWalkSafety(1.8));

    props.setMixinProperties("trail_visibility=intermediate", ofWalkSafety(1.8));

    /*
     * Automobile speeds in Norway.
     * The national speed limit is 80 km/h in rural areas and 50 km/h i urban areas.
     * Normally the speed limit is signed explicit, and the national speed limits don't apply.
     * Design speed for new motorways are 110 km/h, and 90 km/h for motorroads.
     * Legal speed limit for pedestrian and living streets is walking pace.
     */

    props.setCarSpeed(
      new ExactMatchSpecifier(new Condition.OneOf("highway", "motorway", "motorway_link")),
      30.56f // 110 km/t
    );

    props.setCarSpeed(
      new ExactMatchSpecifier(new Condition.Equals("motorroad", "yes"), isTrunkOrPrimary),
      25.f // 90 km/t
    );
    props.setCarSpeed(
      new ExactMatchSpecifier(
        new Condition.OneOf(
          "highway",
          "trunk",
          "trunk_link",
          "primary",
          "primary_link",
          "secondary",
          "secondary_link",
          "tertiary",
          "tertiary_link",
          "unclassified",
          "road",
          "busway"
        )
      ),
      22.22f // 80 km/t
    );
    props.setCarSpeed(
      new ExactMatchSpecifier(
        new Condition.OneOf("sidewalk", "yes", "both", "left", "right", "separate"),
        new Condition.OneOf(
          "highway",
          "trunk",
          "trunk_link",
          "primary",
          "primary_link",
          "secondary",
          "secondary_link",
          "tertiary",
          "tertiary_link",
          "unclassified",
          "road",
          "busway"
        )
      ),
      13.89f // 50 km/t
    );

    props.setCarSpeed("highway=residential", 13.89f); // 50 km/h
    props.setCarSpeed("highway=service", 13.89f); // 50 km/h

    props.setCarSpeed("highway=service;service=driveway", 8.33f); // 30 km/h
    props.setCarSpeed("highway=service;service=parking_aisle", 8.33f);
    props.setCarSpeed("highway=track", 8.33f);

    props.setCarSpeed("highway=living_street", 1.94f); // 7 km/h
    props.setCarSpeed("highway=pedestrian", 1.94f); // 7 km/h
    props.setCarSpeed("highway=footway", 1.94f); // 7 km/h

    props.defaultCarSpeed = 22.22f; // 80 km/h
    props.maxPossibleCarSpeed = 30.56f; // 110 km/h

    super.populateNotesAndNames(props);

    props.setSlopeOverride(new BestMatchSpecifier("bridge=*"), true);
    props.setSlopeOverride(new BestMatchSpecifier("cutting=*"), true);
    props.setSlopeOverride(new BestMatchSpecifier("tunnel=*"), true);
    props.setSlopeOverride(new BestMatchSpecifier("location=underground"), true);
    props.setSlopeOverride(new BestMatchSpecifier("indoor=yes"), true);
  }
}
