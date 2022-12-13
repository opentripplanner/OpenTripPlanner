package org.opentripplanner.openstreetmap.tagmapping;

import static org.opentripplanner.openstreetmap.wayproperty.WayPropertiesBuilder.withModes;
import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;
import static org.opentripplanner.street.model.StreetTraversalPermission.CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.NONE;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;

import java.util.function.BiFunction;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.openstreetmap.wayproperty.WayPropertySet;
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
 * @see DefaultMapper
 */
class FinlandMapper implements OsmTagMapper {

  @Override
  public void populateProperties(WayPropertySet props) {
    BiFunction<StreetTraversalPermission, Float, Double> defaultWalkSafetyForPermission = (
        permission,
        speedLimit
      ) ->
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

    // Do not walk on "moottoriliikennetie"
    props.setProperties("motorroad=yes", withModes(CAR).bicycleSafety(7.47));

    // Remove informal and private roads
    props.setProperties("highway=*;informal=yes", withModes(NONE));
    props.setProperties("highway=service;access=private", withModes(NONE));
    props.setProperties("highway=trail", withModes(NONE));

    // No biking on designated footways/sidewalks
    props.setProperties("highway=footway", withModes(PEDESTRIAN));
    props.setProperties("footway=sidewalk;highway=footway", withModes(PEDESTRIAN).walkSafety(1.0));

    // Prefer designated cycleways
    props.setProperties(
      "highway=cycleway;bicycle=designated",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(0.6)
    );

    // Remove Helsinki city center service tunnel network from graph
    props.setProperties("highway=service;tunnel=yes;access=destination", withModes(NONE));
    props.setProperties("highway=service;access=destination", withModes(ALL).bicycleSafety(1.1));

    /*
     * Automobile speeds in Finland. General speed limit is 80kph unless signs says otherwise.
     */
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

    // Read the rest from the default set
    new DefaultMapper().populateProperties(props);
  }

  @Override
  public boolean isBicycleNoThroughTrafficExplicitlyDisallowed(OSMWithTags way) {
    String bicycle = way.getTag("bicycle");
    return (
      isVehicleThroughTrafficExplicitlyDisallowed(way) ||
      doesTagValueDisallowThroughTraffic(bicycle)
    );
  }

  @Override
  public boolean isWalkNoThroughTrafficExplicitlyDisallowed(OSMWithTags way) {
    String foot = way.getTag("foot");
    return isGeneralNoThroughTraffic(way) || doesTagValueDisallowThroughTraffic(foot);
  }
}
