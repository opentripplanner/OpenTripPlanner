package org.opentripplanner.osm.tagmapping;

import static org.opentripplanner.osm.wayproperty.MixinPropertiesBuilder.ofWalkSafety;
import static org.opentripplanner.osm.wayproperty.WayPropertiesBuilder.withModes;
import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;
import static org.opentripplanner.street.model.StreetTraversalPermission.CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.NONE;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;

import java.util.Set;
import org.opentripplanner.framework.functional.FunctionUtils.TriFunction;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.wayproperty.WayPropertySet;
import org.opentripplanner.street.model.StreetTraversalPermission;

/**
 * OSM way properties for Finnish roads. {@link FinlandMapper} is derived from
 * {@link NorwayMapper} by seime
 * <p>
 * The main difference compared to the default property set is that most of the highway=trunk roads
 * also allows walking and biking, where as some does not. http://wiki.openstreetmap.org/wiki/Tag:highway%3Dtrunk
 * http://wiki.openstreetmap.org/wiki/Highway:International_equivalence
 *
 * @author juusokor
 * @see OsmTagMapper
 */
class FinlandMapper extends OsmTagMapper {

  private static final Set<String> NOTHROUGH_DRIVING_TAGS = Set.of(
    "parking_aisle",
    "driveway",
    "alley",
    "emergency_access",
    "drive-through"
  );

  @Override
  public void populateProperties(WayPropertySet props) {
    TriFunction<
      StreetTraversalPermission,
      Float,
      OsmEntity,
      Double
    > defaultWalkSafetyForPermission = (permission, speedLimit, way) ->
      switch (permission) {
        case ALL, PEDESTRIAN_AND_CAR -> {
          // ~35kph or under
          if (speedLimit <= 9.75f) {
            yield 1.45;
          }
          // ~60kph or under
          else if (speedLimit <= 16.65f) {
            yield 1.6;
          }
          // over ~60kph
          else {
            yield 1.8;
          }
        }
        case PEDESTRIAN_AND_BICYCLE -> 1.15;
        case PEDESTRIAN -> 1.1;
        // these don't include walking
        case BICYCLE_AND_CAR, BICYCLE, CAR, NONE -> 1.8;
      };
    props.setDefaultWalkSafetyForPermission(defaultWalkSafetyForPermission);
    props.setProperties("highway=living_street", withModes(ALL).bicycleSafety(0.9));
    props.setProperties("highway=unclassified", withModes(ALL));
    props.setProperties("highway=road", withModes(ALL));
    props.setProperties("highway=byway", withModes(ALL).bicycleSafety(1.3));
    props.setProperties("highway=track", withModes(ALL).bicycleSafety(1.3));
    props.setProperties("highway=service", withModes(ALL).bicycleSafety(1.1));
    props.setProperties("highway=residential", withModes(ALL).bicycleSafety(0.98));
    props.setProperties("highway=residential_link", withModes(ALL).bicycleSafety(0.98));
    props.setProperties("highway=tertiary", withModes(ALL));
    props.setProperties("highway=tertiary_link", withModes(ALL));
    props.setProperties("highway=secondary", withModes(ALL).bicycleSafety(1.5));
    props.setProperties("highway=secondary_link", withModes(ALL).bicycleSafety(1.5));
    props.setProperties("highway=primary", withModes(ALL).bicycleSafety(2.06));
    props.setProperties("highway=primary_link", withModes(ALL).bicycleSafety(2.06));
    // Replace existing matching properties as the logic is that the first statement registered takes precedence over later statements
    props.setProperties("highway=trunk_link", withModes(ALL).bicycleSafety(2.06));
    props.setProperties("highway=trunk", withModes(ALL).bicycleSafety(7.47));

    // Don't recommend walking in trunk road tunnels
    props.setProperties("highway=trunk;tunnel=yes", withModes(CAR).bicycleSafety(7.47));

    // Remove informal and private roads
    props.setProperties("highway=*;informal=yes", withModes(NONE));
    props.setProperties("highway=service;access=private", withModes(NONE));
    props.setProperties("highway=trail", withModes(NONE));

    // Remove ice/winter roads
    props.setProperties("highway=*;seasonal=winter", withModes(NONE));
    props.setProperties("highway=*;ice_road=yes", withModes(NONE));
    props.setProperties("highway=*;winter_road=yes", withModes(NONE));

    // No biking on designated footways/sidewalks
    props.setProperties("highway=footway", withModes(PEDESTRIAN));
    props.setProperties("footway=sidewalk;highway=footway", withModes(PEDESTRIAN));

    // Walking on segregated ways is safer than when cycling and walking happens on the same lane
    props.setProperties(
      "highway=cycleway;segregated=yes",
      withModes(PEDESTRIAN_AND_BICYCLE).walkSafety(1.1).bicycleSafety(0.6)
    );

    // Tunnels and bridges are safer than crossing a street
    props.setProperties("highway=footway;bridge=yes", withModes(PEDESTRIAN).walkSafety(1.0));
    props.setProperties("highway=footway;tunnel=yes", withModes(PEDESTRIAN).walkSafety(1.0));
    props.setProperties(
      "highway=cycleway;bridge=yes",
      withModes(PEDESTRIAN_AND_BICYCLE).walkSafety(1.0).bicycleSafety(0.6)
    );
    props.setProperties(
      "highway=cycleway;tunnel=yes",
      withModes(PEDESTRIAN_AND_BICYCLE).walkSafety(1.0).bicycleSafety(0.6)
    );

    props.setProperties(
      "highway=footway;footway=crossing;crossing=traffic_signals",
      withModes(PEDESTRIAN).walkSafety(1.1)
    );
    // Crossing is less safe for pedestrians without traffic lights
    props.setProperties("highway=footway;footway=crossing", withModes(PEDESTRIAN).walkSafety(1.2));

    // If cycleway is segregated, walking is as safe on crossing as in footway crossings
    props.setProperties(
      "highway=cycleway;cycleway=crossing;segregated=yes;crossing=traffic_signals",
      withModes(PEDESTRIAN_AND_BICYCLE).walkSafety(1.1).bicycleSafety(0.8)
    );
    props.setProperties(
      "highway=cycleway;footway=crossing;segregated=yes;crossing=traffic_signals",
      withModes(PEDESTRIAN).walkSafety(1.1).bicycleSafety(0.8)
    );
    props.setProperties(
      "highway=cycleway;cycleway=crossing;segregated=yes",
      withModes(PEDESTRIAN_AND_BICYCLE).walkSafety(1.2).bicycleSafety(1.2)
    );
    props.setProperties(
      "highway=cycleway;footway=crossing;segregated=yes",
      withModes(PEDESTRIAN).walkSafety(1.2).bicycleSafety(1.2)
    );

    // If cycleway is not segregated, walking is less safe on crossing than it is footway crossings
    props.setProperties(
      "highway=cycleway;cycleway=crossing;crossing=traffic_signals",
      withModes(PEDESTRIAN_AND_BICYCLE).walkSafety(1.15).bicycleSafety(0.8)
    );
    props.setProperties(
      "highway=cycleway;footway=crossing;crossing=traffic_signals",
      withModes(PEDESTRIAN_AND_BICYCLE).walkSafety(1.15).bicycleSafety(0.8)
    );
    props.setProperties(
      "highway=cycleway;cycleway=crossing",
      withModes(PEDESTRIAN_AND_BICYCLE).walkSafety(1.25).bicycleSafety(1.2)
    );
    props.setProperties(
      "highway=cycleway;footway=crossing",
      withModes(PEDESTRIAN_AND_BICYCLE).walkSafety(1.25).bicycleSafety(1.2)
    );

    // Prefer designated cycleways
    props.setProperties(
      "highway=cycleway;bicycle=designated",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(0.6)
    );

    // Remove Helsinki city center service tunnel network from graph
    props.setProperties("highway=service;tunnel=yes;access=destination", withModes(NONE));
    props.setProperties("highway=service;access=destination", withModes(ALL).bicycleSafety(1.1));

    // Typically if this tag is used on a way, there is also a better option for walking.
    // We don't need to set bicycle safety as cycling is not currently allowed on these ways.
    props.setMixinProperties("bicycle=use_sidepath", ofWalkSafety(5));

    // Automobile speeds in Finland.
    // General speed limit is 80kph unless signs says otherwise.
    props.defaultCarSpeed = 22.22f;
    // 120kph is the max speed limit in Finland
    props.maxPossibleCarSpeed = 33.34f;
    // = 100kph. Varies between 80 - 120 kph depending on road and season.
    props.setCarSpeed("highway=motorway", 27.77f);
    // = 54kph
    props.setCarSpeed("highway=motorway_link", 15);
    // 80kph "Valtatie"
    props.setCarSpeed("highway=trunk", 22.22f);
    // = 54kph
    props.setCarSpeed("highway=trunk_link", 15);
    // 80kph "Kantatie"
    props.setCarSpeed("highway=primary", 22.22f);
    // = 54kph
    props.setCarSpeed("highway=primary_link", 15);
    // ~= 70kph
    props.setCarSpeed("highway=secondary", 19.45f);
    // = 54kph
    props.setCarSpeed("highway=secondary_link", 15);
    // ~= 60kph
    props.setCarSpeed("highway=tertiary", 16.65f);
    // ~= 40 kph
    props.setCarSpeed("highway=tertiary_link", 11.2f);
    props.setCarSpeed("highway=unclassified", 11.2f);
    props.setCarSpeed("highway=road", 11.2f);
    // ~= 35 kph
    props.setCarSpeed("highway=residential", 9.75f);
    // ~= 20 kph
    props.setCarSpeed("highway=service", 5.55f);
    props.setCarSpeed("highway=living_street", 5.55f);
    // ~= 16 kph
    props.setCarSpeed("highway=track", 4.5f);

    super.populateProperties(props);
  }

  @Override
  public boolean isBicycleThroughTrafficExplicitlyDisallowed(OsmEntity way) {
    String bicycle = way.getTag("bicycle");
    return (
      isVehicleThroughTrafficExplicitlyDisallowed(way) ||
      doesTagValueDisallowThroughTraffic(bicycle)
    );
  }

  @Override
  public boolean isWalkThroughTrafficExplicitlyDisallowed(OsmEntity way) {
    String foot = way.getTag("foot");
    return isGeneralNoThroughTraffic(way) || doesTagValueDisallowThroughTraffic(foot);
  }

  @Override
  public boolean isMotorVehicleThroughTrafficExplicitlyDisallowed(OsmEntity way) {
    if (super.isMotorVehicleThroughTrafficExplicitlyDisallowed(way)) {
      return true;
    }
    return way.isOneOfTags("service", NOTHROUGH_DRIVING_TAGS);
  }
}
