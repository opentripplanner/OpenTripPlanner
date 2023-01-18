package org.opentripplanner.openstreetmap.tagmapping;

import static org.opentripplanner.openstreetmap.wayproperty.MixinPropertiesBuilder.ofBicycleSafety;
import static org.opentripplanner.openstreetmap.wayproperty.MixinPropertiesBuilder.ofWalkSafety;
import static org.opentripplanner.openstreetmap.wayproperty.WayPropertiesBuilder.withModes;
import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;
import static org.opentripplanner.street.model.StreetTraversalPermission.BICYCLE_AND_CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.NONE;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;

import java.util.function.BiFunction;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.openstreetmap.wayproperty.WayPropertySet;
import org.opentripplanner.openstreetmap.wayproperty.specifier.BestMatchSpecifier;
import org.opentripplanner.openstreetmap.wayproperty.specifier.Condition;
import org.opentripplanner.openstreetmap.wayproperty.specifier.ExactMatchSpecifier;
import org.opentripplanner.openstreetmap.wayproperty.specifier.LogicalOrSpecifier;

/**
 * OSM way properties for Norwegian roads. The main difference compared to the default property set
 * is that most of the highway=trunk roads also allows walking and biking, where as some does not.
 * http://wiki.openstreetmap.org/wiki/Tag:highway%3Dtrunk http://wiki.openstreetmap.org/wiki/Highway:International_equivalence
 *
 * @author seime
 * @see OsmTagMapper
 * @see DefaultMapper
 */
class NorwayMapper implements OsmTagMapper {

  @Override
  public void populateProperties(WayPropertySet props) {
    var hasSidewalk = new Condition.EqualsAnyIn("sidewalk", "yes", "left", "right", "both");
    var hasPrefixSidewalk = new Condition.Equals("sidewalk", "yes"); // e.g sidewalk:left=yes
    props.setDefaultWalkSafetyForPermission((permission, speedLimit, way) ->
      switch (permission) {
        case ALL, PEDESTRIAN_AND_CAR -> {
          if (
            hasSidewalk.matches(way) ||
            hasPrefixSidewalk.matchesLeft(way) ||
            hasPrefixSidewalk.matchesRight(way)
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

    var isTrunkOrPrimary = new Condition.EqualsAnyIn(
      "highway",
      "trunk",
      "trunk_link",
      "primary",
      "primary_link"
    );
    var isSecondaryHighway = new Condition.EqualsAnyIn("highway", "secondary", "secondary_link");
    var isTertiaryHighway = new Condition.EqualsAnyIn("highway", "tertiary", "tertiary_link");

    BiFunction<Float, OSMWithTags, Double> cycleSafetyHighway = (speedLimit, way) -> {
      // 90 km/h or over
      if (speedLimit >= 25f) {
        return cycleSafetyVeryHighTraffic;
      }
      // ~70 km/h or over
      else if (speedLimit >= 19.4f) {
        if (isTrunkOrPrimary.matches(way)) {
          return cycleSafetyHighTraffic;
        } else return cycleSafetyMediumHighTraffic;
      }
      // between ~60 km/h and ~40 km/
      else if (speedLimit >= 11.1f) {
        if (isTrunkOrPrimary.matches(way)) {
          // 60 km/h or 50 to 40 km/h
          return speedLimit >= 16.6f ? cycleSafetyMediumHighTraffic : cycleSafetyMediumTraffic;
        } else if (isSecondaryHighway.matches(way)) {
          // ~60 km/h or 50 to 40 km/h
          return speedLimit >= 16.6f ? cycleSafetyMediumTraffic : cycleSafetyMediumLowTraffic;
        } else if (isTertiaryHighway.matches(way)) {
          // ~60 to 50 km/h or 40 km/h
          return speedLimit >= 13.8f ? cycleSafetyMediumLowTraffic : cycleSafetyLowTraffic;
        }
      }
      // 30 km/h or lower, or lower road class than unclassified
      if (
        this.isMotorVehicleThroughTrafficExplicitlyDisallowed(way)
      ) return cycleSafetyVeryLowTraffic; else return cycleSafetyLowTraffic;
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
      new ExactMatchSpecifier(new Condition.EqualsAnyIn("highway", "motorway", "motorway_link")),
      withModes(CAR)
    );

    // Walking and cycling illegal on "Motortrafikkvei"
    props.setProperties(
      new ExactMatchSpecifier(isTrunkOrPrimary, new Condition.Equals("motorroad", "yes")),
      withModes(CAR)
    );

    props.setProperties(
      new ExactMatchSpecifier(
        new Condition.EqualsAnyIn(
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
        )
      ),
      withModes(ALL)
    );

    /* bicycle infrastructure */

    var isClassifiedHighway = new Condition.EqualsAnyIn(
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

    props.setProperties(
      new ExactMatchSpecifier(
        new Condition.Equals("cycleway", "track"),
        new Condition.EqualsAnyIn(
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
          "residential",
          "living_street"
        )
      ),
      withModes(ALL).bicycleSafety(1.1)
    );

    props.setProperties(
      new ExactMatchSpecifier(new Condition.Equals("cycleway", "lane"), isClassifiedHighway),
      withModes(ALL).bicycleSafety(1.27)
    );

    props.setProperties(
      new ExactMatchSpecifier(
        new Condition.Equals("cycleway", "lane"),
        new Condition.LessThan("maxspeed", 50),
        isClassifiedHighway
      ),
      withModes(ALL).bicycleSafety(1.1)
    );

    props.setProperties(
      new ExactMatchSpecifier(
        new Condition.Equals("cycleway", "lane"),
        new Condition.EqualsAnyIn("highway", "unclassified", "residential")
      ),
      withModes(ALL).bicycleSafety(1.1)
    );

    props.setMixinProperties(
      new ExactMatchSpecifier(
        new Condition.Equals("oneway", "yes"),
        new Condition.EqualsAnyInOrAbsent("cycleway"),
        new Condition.EqualsAnyIn(
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
        )
      ),
      ofBicycleSafety(1, 1.15)
    );

    var isClassifiedOrUnclassifiedHighway = new Condition.EqualsAnyIn(
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

    // Discourage cycling on roads with no infrastructure for neither walking nor cycling
    props.setProperties(
      new ExactMatchSpecifier(
        new Condition.Equals("foot", "no"),
        isClassifiedOrUnclassifiedHighway
      ),
      withModes(BICYCLE_AND_CAR).bicycleSafety(cycleSafetyVeryHighTraffic)
    );

    // Discourage cycling and walking in road tunnels
    props.setMixinProperties(
      new ExactMatchSpecifier(
        new Condition.Equals("tunnel", "yes"),
        isClassifiedOrUnclassifiedHighway
      ),
      ofBicycleSafety(2.).ofWalkSafety(2.)
    );

    // Discourage walking in on a bridge and other ways without a verge without a sidewalk
    props.setMixinProperties(
      new LogicalOrSpecifier(
        new ExactMatchSpecifier(
          new Condition.Equals("bridge", "yes"),
          new Condition.EqualsAnyInOrAbsent("sidewalk", "no", "separate"),
          isClassifiedOrUnclassifiedHighway
        ),
        new ExactMatchSpecifier(
          new Condition.Equals("verge", "no"),
          new Condition.EqualsAnyInOrAbsent("sidewalk", "no", "separate"),
          isClassifiedOrUnclassifiedHighway
        )
      ),
      ofWalkSafety(2.)
    );

    // Discourage walking in roundabouts
    props.setMixinProperties(
      new ExactMatchSpecifier(
        new Condition.Equals("junction", "roundabout"),
        new Condition.EqualsAnyInOrAbsent("sidewalk", "no", "separate")
      ),
      ofWalkSafety(2.)
    );

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
        new Condition.EqualsAnyIn("bus", "yes", "designated")
      ),
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(cycleSafetyMediumLowTraffic).walkSafety(1.9)
    );

    /* Footway and cycleway */
    var footway = withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.42).walkSafety(1.);
    var cycleway = withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.05).walkSafety(1.4);
    var cyclewayWithSidewalk = withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.05).walkSafety(1.);
    var twoLaneOrOnewayCycleway = withModes(PEDESTRIAN_AND_BICYCLE)
      .bicycleSafety(1.)
      .walkSafety(1.4);
    var twoLaneOrOnewayCyclewayWithSidewalk = withModes(PEDESTRIAN_AND_BICYCLE)
      .bicycleSafety(1.)
      .walkSafety(1.);
    var combinedFootAndCycleway = withModes(PEDESTRIAN_AND_BICYCLE);

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

    props.setProperties(
      "highway=track",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.1).walkSafety(1.3)
    );

    props.setProperties(
      "highway=track;tracktype=grade1",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.57).walkSafety(1.1)
    );
    props.setProperties(
      "highway=track;tracktype=grade2",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.83).walkSafety(1.5)
    );
    props.setProperties(
      "highway=track;tracktype=grade3",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(2.5).walkSafety(1.65)
    );
    props.setProperties(
      "highway=track;tracktype=grade4",
      withModes(PEDESTRIAN).bicycleSafety(2.5).walkSafety(1.8)
    );
    props.setProperties(
      "highway=track;tracktype=grade5",
      withModes(PEDESTRIAN).bicycleSafety(2.5).walkSafety(1.8)
    );
    props.setProperties(
      "highway=bridleway",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.1).walkSafety(1.5)
    );
    props.setProperties(
      "highway=path",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.5).walkSafety(1.8)
    );
    props.setProperties("highway=steps", withModes(PEDESTRIAN));
    props.setProperties("highway=corridor", withModes(PEDESTRIAN));
    props.setProperties("highway=footway;indoor=yes", withModes(PEDESTRIAN));
    props.setProperties("highway=platform", withModes(PEDESTRIAN));
    props.setProperties("public_transport=platform", withModes(PEDESTRIAN));

    props.setMixinProperties("smoothness=intermediate", ofBicycleSafety(1.5));
    props.setMixinProperties("smoothness=bad", ofBicycleSafety(2));
    props.setProperties("highway=*;smoothness=very_bad", withModes(PEDESTRIAN));
    props.setProperties(
      new ExactMatchSpecifier(
        new Condition.EqualsAnyIn("smoothness", "horrible", "very_horrible", "impassable"),
        new Condition.EqualsAnyIn("highway", "path", "bridleway", "track")
      ),
      withModes(NONE)
    );

    props.setProperties("highway=*;mtb:scale=1", withModes(PEDESTRIAN));
    props.setProperties("highway=*;mtb:scale=2", withModes(PEDESTRIAN));
    props.setProperties(
      new ExactMatchSpecifier(
        new Condition.GreaterThan("mtb:scale", 2),
        new Condition.EqualsAnyIn("highway", "path", "bridleway", "track")
      ),
      withModes(NONE)
    );

    props.setProperties(
      "highway=track;tracktype=grade1",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic)
    );
    props.setProperties(
      "highway=track;tracktype=grade2",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.83)
    );
    props.setProperties(
      "highway=track;tracktype=grade3",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(2.5)
    );
    props.setProperties("highway=track;tracktype=grade4", withModes(PEDESTRIAN));
    props.setProperties("highway=track;tracktype=grade5", withModes(PEDESTRIAN));

    props.setProperties("highway=path;trail_visibility=bad", withModes(NONE));
    props.setProperties("highway=path;trail_visibility=no", withModes(NONE));
    props.setProperties("highway=path;trail_visibility=low", withModes(NONE));
    props.setProperties("highway=path;trail_visibility=poor", withModes(NONE));

    props.setProperties("highway=path;sac_scale=mountain_hiking", withModes(NONE));
    props.setProperties("highway=path;sac_scale=demanding_mountain_hiking", withModes(NONE));
    props.setProperties("highway=path;sac_scale=alpine_hiking", withModes(NONE));
    props.setProperties("highway=path;sac_scale=demanding_alpine_hiking", withModes(NONE));
    props.setProperties("highway=path;sac_scale=difficult_alpine_hiking", withModes(NONE));

    // Not expected to be paved
    props.setProperties(
      new ExactMatchSpecifier(
        new Condition.EqualsAnyIn(
          "surface",
          "asfalt",
          "concrete",
          "sett",
          "paving_stones",
          "grass_paver",
          "paved",
          "wood",
          "metal_grid",
          "metal"
        ),
        new Condition.Absent("trackgrade"),
        new Condition.EqualsAnyIn("highway", "path", "bridleway", "track")
      ),
      withModes(PEDESTRIAN_AND_BICYCLE)
    );

    // Paved but unfavorable for bicycles
    props.setMixinProperties("surface=grass_paver", ofBicycleSafety(1.2));
    props.setMixinProperties("surface=sett", ofBicycleSafety(1.2));
    props.setMixinProperties("surface=cobblestone", ofBicycleSafety(1.2));
    props.setMixinProperties("surface=unhewn_cobblestone", ofBicycleSafety(2));
    // Slick if wet
    props.setMixinProperties("surface=metal_grid", ofBicycleSafety(1.2));
    props.setMixinProperties("surface=metal", ofBicycleSafety(1.2));

    // Unpaved
    props.setMixinProperties("surface=unpaved", ofBicycleSafety(1.2).walkSafety(1.2));
    props.setMixinProperties("surface=compacted", ofBicycleSafety(1.2).walkSafety(1.2));
    props.setMixinProperties("surface=fine_gravel", ofBicycleSafety(1.3).walkSafety(1.2));
    props.setMixinProperties("surface=pebblestone", ofBicycleSafety(1.3).walkSafety(1.2));
    props.setMixinProperties("surface=gravel", ofBicycleSafety(1.3).walkSafety(1.2));
    props.setMixinProperties("surface=woodchip", ofBicycleSafety(1.5).walkSafety(1.2));
    props.setMixinProperties("surface=ground", ofBicycleSafety(1.5).walkSafety(1.5));
    props.setMixinProperties("surface=dirt", ofBicycleSafety(1.5).walkSafety(1.5));
    props.setMixinProperties("surface=earth", ofBicycleSafety(1.5).walkSafety(1.5));
    props.setMixinProperties("surface=grass", ofBicycleSafety(1.5).walkSafety(1.2));
    props.setMixinProperties("surface=mud", ofBicycleSafety(2).walkSafety(2));
    props.setMixinProperties("surface=sand", ofBicycleSafety(2).walkSafety(1.2));

    //relation properties are copied over to ways
    props.setMixinProperties(
      new LogicalOrSpecifier("lcn=yes", "rcn=yes", "ncn=yes"),
      ofBicycleSafety(0.85)
    );

    props.setMixinProperties(
      new ExactMatchSpecifier(
        new Condition.EqualsAnyIn("embedded_rails", "tram", "light_rail", "disused")
      ),
      ofBicycleSafety(1.2)
    );

    /*
     * Automobile speeds in Norway.
     * The national speed limit is 80 km/h in rural areas and 50 km/h i urban areas.
     * Normally the speed limit is signed explicit, and the national speed limits don't apply.
     * Design speed for new motorways are 110 km/h, and 90 km/h for motorroads.
     * Legal speed limit for pedestrian and living streets is walking pace.
     */

    props.setCarSpeed(
      new ExactMatchSpecifier(new Condition.EqualsAnyIn("highway", "motorway", "motorway_link")),
      30.56f // 110 km/t
    );
    props.setCarSpeed(
      new ExactMatchSpecifier(new Condition.Equals("motorroad", "yes"), isTrunkOrPrimary),
      25.f // 90 km/t
    );
    props.setCarSpeed(
      new ExactMatchSpecifier(
        new Condition.EqualsAnyIn(
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
        new Condition.EqualsAnyIn("sidewalk", "yes", "both", "left", "right", "separate"),
        new Condition.EqualsAnyIn(
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

    props.setCarSpeed("highway=residential", 13.89f); // 50 km/t
    props.setCarSpeed("highway=service", 13.89f); // 50 km/t

    props.setCarSpeed("highway=service;service=driveway", 8.33f); // 30 km/t
    props.setCarSpeed("highway=service;service=parking_aisle", 8.33f);
    props.setCarSpeed("highway=track", 8.33f);

    props.setCarSpeed("highway=living_street", 1.94f); // 7 km/t
    props.setCarSpeed("highway=pedestrian", 1.94f); // 7 km/t

    props.defaultSpeed = 22.22f; // 80 km/t

    new DefaultMapper().populateNotesAndNames(props);

    props.setSlopeOverride(new BestMatchSpecifier("bridge=*"), true);
    props.setSlopeOverride(new BestMatchSpecifier("cutting=*"), true);
    props.setSlopeOverride(new BestMatchSpecifier("tunnel=*"), true);
    props.setSlopeOverride(new BestMatchSpecifier("location=underground"), true);
    props.setSlopeOverride(new BestMatchSpecifier("indoor=yes"), true);
  }
}
