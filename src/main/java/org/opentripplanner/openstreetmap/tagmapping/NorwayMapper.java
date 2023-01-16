package org.opentripplanner.openstreetmap.tagmapping;

import static org.opentripplanner.openstreetmap.wayproperty.MixinPropertiesBuilder.ofBicycleSafety;
import static org.opentripplanner.openstreetmap.wayproperty.WayPropertiesBuilder.withModes;
import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;
import static org.opentripplanner.street.model.StreetTraversalPermission.BICYCLE;
import static org.opentripplanner.street.model.StreetTraversalPermission.BICYCLE_AND_CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.NONE;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN_AND_CAR;

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
    var very_high_traffic = 10.;
    var high_traffic = 3.75;
    var medium_high_traffic = 3.43;
    var medium_traffic = 2.5;
    var medium_low_traffic = 2.37;
    var low_traffic = 1.83;
    var very_low_traffic = 1.57;

    var cycle_lane_medium_traffic = 1.27;
    var cycle_lane_low_traffic = 1.10;

    var dedicated_footway = 1.42;
    var mixed_cycleway = 1.12;
    var dedicated_cycleway = 1.05;
    var dual_lane_or_oneway_cycleway = 1;

    var trunkOrPrimary = new Condition.EqualsAnyIn(
      "highway",
      "trunk",
      "trunk_link",
      "primary",
      "primary_link"
    );
    var secondaryHighway = new Condition.EqualsAnyIn("highway", "secondary", "secondary_link");
    var tertiaryHighway = new Condition.EqualsAnyIn("highway", "tertiary", "tertiary_link");

    BiFunction<Float, OSMWithTags, Double> cycleSafetyHighway = (speedLimit, way) -> {
      // 90 km/h or over
      if (speedLimit >= 25f) {
        return very_high_traffic;
      }
      // ~70 km/h or over
      else if (speedLimit >= 19.4f) {
        if (trunkOrPrimary.matches(way)) {
          return high_traffic;
        } else return medium_high_traffic;
      }
      // between ~60 km/h and ~40 km/
      else if (speedLimit >= 11.1f) {
        if (trunkOrPrimary.matches(way)) {
          // 60 km/h or 50 to 40
          return speedLimit >= 16.6f ? medium_high_traffic : medium_traffic;
        } else if (secondaryHighway.matches(way)) {
          // ~60 km/h or 50 to 40
          return speedLimit >= 16.6f ? medium_traffic : medium_low_traffic;
        } else if (tertiaryHighway.matches(way)) {
          // ~60 to 50 km/h or 40
          return speedLimit >= 13.8f ? medium_low_traffic : low_traffic;
        }
      }
      // ~30 km/h or lower, or lower road class than unclassified
      if (
        this.isMotorVehicleThroughTrafficExplicitlyDisallowed(way)
      ) return very_low_traffic; else return low_traffic;
    };

    props.setDefaultBicycleSafetyForPermission((permission, speedLimit, way) ->
      switch (permission) {
        case ALL -> cycleSafetyHighway.apply(speedLimit, way);
        case BICYCLE_AND_CAR -> very_high_traffic;
        case PEDESTRIAN_AND_BICYCLE -> mixed_cycleway;
        case BICYCLE -> dedicated_cycleway;
        // these don't include cycling
        case PEDESTRIAN_AND_CAR, PEDESTRIAN, CAR, NONE -> very_high_traffic;
      }
    );

    props.setProperties(
      new ExactMatchSpecifier(new Condition.EqualsAnyIn("highway", "motorway", "motorway_link")),
      withModes(CAR)
    );

    // Walking and cycling illegal on "Motortrafikkvei"
    props.setProperties(
      new ExactMatchSpecifier(trunkOrPrimary, new Condition.Equals("motorroad", "yes")),
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
      withModes(ALL).bicycleSafety(dual_lane_or_oneway_cycleway)
    );

    props.setProperties(
      new ExactMatchSpecifier(
        new Condition.Equals("cycleway", "lane"),
        new Condition.EqualsAnyIn(
          "highway",
          "trunk",
          "trunk_link",
          "primary",
          "primary_link",
          "secondary",
          "secondary_link",
          "tertiary",
          "tertiary_link"
        )
      ),
      withModes(ALL).bicycleSafety(cycle_lane_medium_traffic)
    );

    props.setProperties(
      new ExactMatchSpecifier(
        new Condition.Equals("cycleway", "lane"),
        new Condition.LessThan("maxspeed", 50),
        new Condition.EqualsAnyIn(
          "highway",
          "trunk",
          "trunk_link",
          "primary",
          "primary_link",
          "secondary",
          "secondary_link",
          "tertiary",
          "tertiary_link"
        )
      ),
      withModes(ALL).bicycleSafety(cycle_lane_low_traffic)
    );

    props.setProperties(
      new ExactMatchSpecifier(
        new Condition.Equals("cycleway", "lane"),
        new Condition.EqualsAnyIn("highway", "unclassified", "residential")
      ),
      withModes(ALL).bicycleSafety(cycle_lane_low_traffic)
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

    // Discourage cycling on roads with no infrastructure for neither walking nor cycling
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
          "unclassified"
        ),
        new Condition.Equals("foot", "no")
      ),
      withModes(BICYCLE_AND_CAR).bicycleSafety(very_high_traffic)
    );

    // Discourage cycling on trunk road tunnels
    props.setMixinProperties(
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
          "unclassified"
        ),
        new Condition.Equals("tunnel", "yes")
      ),
      ofBicycleSafety(1.2)
    );

    // Cycling around reversing cars on a parking lot feels unsafe
    props.setProperties(
      "highway=service;service=parking_aisle",
      withModes(ALL).bicycleSafety(medium_traffic)
    );
    props.setProperties(
      "highway=service;service=drive-through",
      withModes(ALL).bicycleSafety(medium_traffic)
    );

    /* Pedestrian, living and cyclestreet */
    props.setProperties("highway=living_street", withModes(ALL).bicycleSafety(low_traffic));
    props.setProperties("highway=pedestrian", withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.2));

    props.setProperties(
      "highway=footway",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(dedicated_footway)
    );
    // "motor_vehicle=destination" indicates unwanted car traffic, signposted "Kjøring til eiendommene tillatt"
    props.setProperties(
      "highway=footway;motor_vehicle=destination",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic)
    );

    props.setProperties(
      "highway=footway;footway=sidewalk",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.93)
    );
    props.setProperties(
      "highway=footway;footway=crossing",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(2.33)
    );
    props.setProperties(
      "highway=cycleway;cycleway=crossing",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(2.33)
    );

    props.setProperties(
      "highway=cycleway",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(dedicated_cycleway)
    );
    props.setProperties(
      new ExactMatchSpecifier(
        new Condition.Equals("highway", "cycleway"),
        new Condition.GreaterThan("lanes", 1)
      ),
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(dual_lane_or_oneway_cycleway)
    );
    props.setProperties(
      "highway=cycleway;oneway=yes",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(dual_lane_or_oneway_cycleway)
    );
    // "motor_vehicle=destination" indicates unwanted car traffic, signposted "Kjøring til eiendommene tillatt"
    props.setProperties(
      "highway=cycleway;motor_vehicle=destination",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic)
    );

    // segregated=no takes' precedence if there is no "segregated" key. There is no penalty for a tag mismatch
    props.setProperties(
      "highway=cycleway;foot=designated;segregated=no",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(mixed_cycleway)
    );
    props.setProperties(
      "highway=cycleway;foot=designated;segregated=yes",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(dedicated_cycleway)
    );
    props.setProperties(
      "highway=cycleway;foot=designated;segregated=yes;lanes=2",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(dual_lane_or_oneway_cycleway)
    );
    props.setProperties(
      "highway=path;foot=designated;bicycle=designated;segregated=no",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(mixed_cycleway)
    );
    props.setProperties(
      "highway=path;foot=designated;bicycle=designated;segregated=yes",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(dedicated_cycleway)
    );
    // "motor_vehicle=destination" indicates unwanted car traffic, signposted "Kjøring til eiendommene tillatt"
    props.setProperties(
      "highway=cycleway;foot=designated;segregated=*;motor_vehicle=destination",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic)
    );
    props.setProperties(
      "highway=path;foot=designated;bicycle=designated;segregated=*;motor_vehicle=destination",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(very_low_traffic)
    );

    //relation properties are copied over to ways
    props.setMixinProperties(
      new LogicalOrSpecifier("lcn=yes", "rcn=yes", "ncn=yes"),
      ofBicycleSafety(0.85)
    );

    props.setProperties(
      "highway=busway",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(low_traffic)
    );
    props.setProperties("highway=track", withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.1));
    props.setProperties("highway=bridleway", withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.1));
    props.setProperties("highway=path", withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.5));
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

    // paved but unfavorable
    props.setMixinProperties("surface=grass_paver", ofBicycleSafety(1.2));
    props.setMixinProperties("surface=sett", ofBicycleSafety(1.2));
    props.setMixinProperties("surface=cobblestone", ofBicycleSafety(1.2));
    props.setMixinProperties("surface=unhewn_cobblestone", ofBicycleSafety(1.5));
    // Can be slick if wet, but otherwise not unfavorable to bikes
    props.setMixinProperties("surface=metal_grid", ofBicycleSafety(1.2));
    props.setMixinProperties("surface=metal", ofBicycleSafety(1.2));

    // unpaved
    props.setMixinProperties("surface=unpaved", ofBicycleSafety(1.2));
    props.setMixinProperties("surface=compacted", ofBicycleSafety(1.2));
    props.setMixinProperties("surface=fine_gravel", ofBicycleSafety(1.3));
    props.setMixinProperties("surface=pebblestone", ofBicycleSafety(1.3));
    props.setMixinProperties("surface=gravel", ofBicycleSafety(1.3));
    props.setMixinProperties("surface=woodchip", ofBicycleSafety(1.5));
    props.setMixinProperties("surface=ground", ofBicycleSafety(1.5));
    props.setMixinProperties("surface=dirt", ofBicycleSafety(1.5));
    props.setMixinProperties("surface=earth", ofBicycleSafety(1.5));
    props.setMixinProperties("surface=grass", ofBicycleSafety(1.5));
    props.setMixinProperties("surface=mud", ofBicycleSafety(2));
    props.setMixinProperties("surface=sand", ofBicycleSafety(2));

    props.setMixinProperties(
      new ExactMatchSpecifier(
        new Condition.EqualsAnyIn("embedded_rails", "tram", "light_rail", "disused")
      ),
      ofBicycleSafety(1.2)
    );

    /*
     * Automobile speeds in Norway. General speed limit is 80kph unless signs says otherwise
     *
     */

    props.setCarSpeed(
      new ExactMatchSpecifier(new Condition.EqualsAnyIn("highway", "motorway", "motorway_link")),
      30.56f // 110 km/t
    );
    props.setCarSpeed(
      new ExactMatchSpecifier(
        new Condition.EqualsAnyIn("highway", "trunk", "trunk_link", "primary", "primary_link"),
        new Condition.Equals("motorroad", "yes")
      ),
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
          "unclassified"
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
          "unclassified"
        )
      ),
      13.89f // 50 km/t
    );
    props.setCarSpeed("highway=living_street", 1.94f); // 7 km/t
    props.setCarSpeed("highway=pedestrian", 1.94f); // 7 km/t

    props.setCarSpeed("highway=residential", 13.89f); // 50 km/t
    props.setCarSpeed("highway=service", 13.89f); // 50 km/t
    props.setCarSpeed("highway=track", 8.33f); // 30 km/t
    props.setCarSpeed("highway=road", 13.89f); // 50 km/t

    props.defaultSpeed = 22.22f; // 80kph

    new DefaultMapper().populateNotesAndNames(props);

    props.setSlopeOverride(new BestMatchSpecifier("bridge=*"), true);
    props.setSlopeOverride(new BestMatchSpecifier("embankment=*"), false);
    props.setSlopeOverride(new BestMatchSpecifier("tunnel=*"), true);
    props.setSlopeOverride(new BestMatchSpecifier("location=underground"), true);
    props.setSlopeOverride(new BestMatchSpecifier("indoor=yes"), true);
  }
}
