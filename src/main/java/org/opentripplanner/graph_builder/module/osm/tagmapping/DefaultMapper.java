package org.opentripplanner.graph_builder.module.osm.tagmapping;

import static org.opentripplanner.graph_builder.module.osm.WayPropertiesBuilder.withModes;
import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;
import static org.opentripplanner.street.model.StreetTraversalPermission.BICYCLE_AND_CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.NONE;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;

import org.opentripplanner.graph_builder.module.osm.WayProperties;
import org.opentripplanner.graph_builder.module.osm.WayPropertySet;
import org.opentripplanner.graph_builder.module.osm.specifier.BestMatchSpecifier;
import org.opentripplanner.graph_builder.module.osm.specifier.LogicalOrSpecifier;
import org.opentripplanner.routing.services.notes.StreetNotesService;

/**
 * This factory class provides a default collection of {@link WayProperties} that determine how OSM
 * streets can be traversed in various modes.
 * <p>
 * Circa January 2011, Grant and Mele at TriMet undertook proper testing of bike (and transit)
 * routing, and worked with David Turner on assigning proper weights to different facility types.
 * The weights in this file grew organically from trial and error, and are the result of months of
 * testing and tweaking the routes that OTP returned, as well as actually walking/biking these
 * routes and making changes based on those experiences. This set of weights should be a great
 * starting point for others to use, but they are to some extent tailored to the situation in
 * Portland and people shouldn't hesitate to adjust them to for their own instance.
 * <p>
 * The rules for assigning WayProperties to OSM ways are explained in. The final tie breaker if two
 * Pickers both match is the sequence that the properties are added in this file: if all else is
 * equal the 'props.setProperties' statement that is closer to the top of the page will prevail over
 * those lower down the page.
 * <p>
 * Foot and bicycle permissions are also addressed in OpenStreetMapGraphBuilderImpl.Handler#getPermissionsForEntity().
 * For instance, if a way that normally does not permit walking based on its tag matches (the
 * prevailing 'props.setProperties' statement) has a 'foot=yes' tag the permissions are overridden
 * and walking is allowed on that way.
 * <p>
 * TODO clarify why this needs a separate factory interface.
 *
 * @author bdferris, novalis
 * @see OsmTagMapper
 */
public class DefaultMapper implements OsmTagMapper {

  /* Populate properties on existing WayPropertySet */
  public void populateProperties(WayPropertySet props) {
    WayProperties allWayProperties = withModes(ALL).build();
    WayProperties noneWayProperties = withModes(NONE).build();
    WayProperties pedestrianWayProperties = withModes(PEDESTRIAN).build();
    WayProperties pedestrianAndBicycleWayProperties = withModes(PEDESTRIAN_AND_BICYCLE).build();
    /* no bicycle tags */

    /* NONE */
    props.setProperties("mtb:scale=3", noneWayProperties);
    props.setProperties("mtb:scale=4", noneWayProperties);
    props.setProperties("mtb:scale=5", noneWayProperties);
    props.setProperties("mtb:scale=6", noneWayProperties);

    /* PEDESTRIAN */
    props.setProperties("highway=corridor", pedestrianWayProperties);
    props.setProperties("highway=steps", pedestrianWayProperties);
    props.setProperties("highway=crossing", pedestrianWayProperties);
    props.setProperties("highway=platform", pedestrianWayProperties);
    props.setProperties("public_transport=platform", pedestrianWayProperties);
    props.setProperties("railway=platform", pedestrianWayProperties);
    props.setProperties("footway=sidewalk;highway=footway", pedestrianWayProperties);
    props.setProperties("mtb:scale=1", pedestrianWayProperties);
    props.setProperties("mtb:scale=2", pedestrianWayProperties);

    /* PEDESTRIAN_AND_BICYCLE */
    props.setProperties("mtb:scale=0", pedestrianAndBicycleWayProperties);
    props.setProperties("highway=cycleway", withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(0.6));
    props.setProperties("highway=path", withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(0.75));
    props.setProperties("highway=pedestrian", withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(0.9));
    props.setProperties("highway=footway", withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.1));
    props.setProperties("highway=bridleway", withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.3));

    /* ALL */
    props.setProperties("highway=living_street", withModes(ALL).bicycleSafety(0.9));
    props.setProperties("highway=unclassified", allWayProperties);
    props.setProperties("highway=road", allWayProperties);
    props.setProperties("highway=byway", withModes(ALL).bicycleSafety(1.3));
    props.setProperties("highway=track", withModes(ALL).bicycleSafety(1.3));
    props.setProperties("highway=service", withModes(ALL).bicycleSafety(1.1));
    props.setProperties("highway=residential", withModes(ALL).bicycleSafety(0.98));
    props.setProperties("highway=residential_link", withModes(ALL).bicycleSafety(0.98));
    props.setProperties("highway=tertiary", allWayProperties);
    props.setProperties("highway=tertiary_link", allWayProperties);
    props.setProperties("highway=secondary", withModes(ALL).bicycleSafety(1.5));
    props.setProperties("highway=secondary_link", withModes(ALL).bicycleSafety(1.5));
    props.setProperties("highway=primary", withModes(ALL).bicycleSafety(2.06));
    props.setProperties("highway=primary_link", withModes(ALL).bicycleSafety(2.06));

    /* DRIVING ONLY */
    // trunk and motorway links are often short distances and necessary connections
    props.setProperties("highway=trunk_link", withModes(CAR).bicycleSafety(2.06));
    props.setProperties("highway=motorway_link", withModes(CAR).bicycleSafety(2.06));

    props.setProperties("highway=trunk", withModes(CAR).bicycleSafety(7.47));
    props.setProperties("highway=motorway", withModes(CAR).bicycleSafety(8));

    /* cycleway=lane */
    props.setProperties(
      "highway=*;cycleway=lane",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(0.87)
    );
    props.setProperties("highway=service;cycleway=lane", withModes(ALL).bicycleSafety(0.77));
    props.setProperties("highway=residential;cycleway=lane", withModes(ALL).bicycleSafety(0.77));
    props.setProperties(
      "highway=residential_link;cycleway=lane",
      withModes(ALL).bicycleSafety(0.77)
    );
    props.setProperties("highway=tertiary;cycleway=lane", withModes(ALL).bicycleSafety(0.87));
    props.setProperties("highway=tertiary_link;cycleway=lane", withModes(ALL).bicycleSafety(0.87));
    props.setProperties("highway=secondary;cycleway=lane", withModes(ALL).bicycleSafety(0.96));
    props.setProperties("highway=secondary_link;cycleway=lane", withModes(ALL).bicycleSafety(0.96));
    props.setProperties("highway=primary;cycleway=lane", withModes(ALL).bicycleSafety(1.15));
    props.setProperties("highway=primary_link;cycleway=lane", withModes(ALL).bicycleSafety(1.15));

    /* BICYCLE_AND_CAR */
    props.setProperties(
      "highway=trunk;cycleway=lane",
      withModes(BICYCLE_AND_CAR).bicycleSafety(1.5)
    );
    props.setProperties(
      "highway=trunk_link;cycleway=lane",
      withModes(BICYCLE_AND_CAR).bicycleSafety(1.15)
    );
    props.setProperties(
      "highway=motorway;cycleway=lane",
      withModes(BICYCLE_AND_CAR).bicycleSafety(2)
    );
    props.setProperties(
      "highway=motorway_link;cycleway=lane",
      withModes(BICYCLE_AND_CAR).bicycleSafety(1.15)
    );

    /* cycleway=share_busway */
    props.setProperties(
      "highway=*;cycleway=share_busway",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(0.92)
    );
    props.setProperties(
      "highway=service;cycleway=share_busway",
      withModes(ALL).bicycleSafety(0.85)
    );
    props.setProperties(
      "highway=residential;cycleway=share_busway",
      withModes(ALL).bicycleSafety(0.85)
    );
    props.setProperties(
      "highway=residential_link;cycleway=share_busway",
      withModes(ALL).bicycleSafety(0.85)
    );
    props.setProperties(
      "highway=tertiary;cycleway=share_busway",
      withModes(ALL).bicycleSafety(0.92)
    );
    props.setProperties(
      "highway=tertiary_link;cycleway=share_busway",
      withModes(ALL).bicycleSafety(0.92)
    );
    props.setProperties(
      "highway=secondary;cycleway=share_busway",
      withModes(ALL).bicycleSafety(0.99)
    );
    props.setProperties(
      "highway=secondary_link;cycleway=share_busway",
      withModes(ALL).bicycleSafety(0.99)
    );
    props.setProperties(
      "highway=primary;cycleway=share_busway",
      withModes(ALL).bicycleSafety(1.25)
    );
    props.setProperties(
      "highway=primary_link;cycleway=share_busway",
      withModes(ALL).bicycleSafety(1.25)
    );
    props.setProperties(
      "highway=trunk;cycleway=share_busway",
      withModes(BICYCLE_AND_CAR).bicycleSafety(1.75)
    );
    props.setProperties(
      "highway=trunk_link;cycleway=share_busway",
      withModes(BICYCLE_AND_CAR).bicycleSafety(1.25)
    );
    props.setProperties(
      "highway=motorway;cycleway=share_busway",
      withModes(BICYCLE_AND_CAR).bicycleSafety(2.5)
    );
    props.setProperties(
      "highway=motorway_link;cycleway=share_busway",
      withModes(BICYCLE_AND_CAR).bicycleSafety(1.25)
    );

    /* cycleway=opposite_lane */
    props.setProperties(
      "highway=*;cycleway=opposite_lane",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1, 0.87)
    );
    props.setProperties(
      "highway=service;cycleway=opposite_lane",
      withModes(ALL).bicycleSafety(1.1, 0.77)
    );
    props.setProperties(
      "highway=residential;cycleway=opposite_lane",
      withModes(ALL).bicycleSafety(0.98, 0.77)
    );
    props.setProperties(
      "highway=residential_link;cycleway=opposite_lane",
      withModes(ALL).bicycleSafety(0.98, 0.77)
    );
    props.setProperties(
      "highway=tertiary;cycleway=opposite_lane",
      withModes(ALL).bicycleSafety(1, 0.87)
    );
    props.setProperties(
      "highway=tertiary_link;cycleway=opposite_lane",
      withModes(ALL).bicycleSafety(1, 0.87)
    );
    props.setProperties(
      "highway=secondary;cycleway=opposite_lane",
      withModes(ALL).bicycleSafety(1.5, 0.96)
    );
    props.setProperties(
      "highway=secondary_link;cycleway=opposite_lane",
      withModes(ALL).bicycleSafety(1.5, 0.96)
    );
    props.setProperties(
      "highway=primary;cycleway=opposite_lane",
      withModes(ALL).bicycleSafety(2.06, 1.15)
    );
    props.setProperties(
      "highway=primary_link;cycleway=opposite_lane",
      withModes(ALL).bicycleSafety(2.06, 1.15)
    );
    props.setProperties(
      "highway=trunk;cycleway=opposite_lane",
      withModes(BICYCLE_AND_CAR).bicycleSafety(7.47, 1.5)
    );
    props.setProperties(
      "highway=trunk_link;cycleway=opposite_lane",
      withModes(BICYCLE_AND_CAR).bicycleSafety(2.06, 1.15)
    );

    /* cycleway=track */
    props.setProperties(
      "highway=*;cycleway=track",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(0.75)
    );
    props.setProperties("highway=service;cycleway=track", withModes(ALL).bicycleSafety(0.65));
    props.setProperties("highway=residential;cycleway=track", withModes(ALL).bicycleSafety(0.65));
    props.setProperties(
      "highway=residential_link;cycleway=track",
      withModes(ALL).bicycleSafety(0.65)
    );
    props.setProperties("highway=tertiary;cycleway=track", withModes(ALL).bicycleSafety(0.75));
    props.setProperties("highway=tertiary_link;cycleway=track", withModes(ALL).bicycleSafety(0.75));
    props.setProperties("highway=secondary;cycleway=track", withModes(ALL).bicycleSafety(0.8));
    props.setProperties("highway=secondary_link;cycleway=track", withModes(ALL).bicycleSafety(0.8));
    props.setProperties("highway=primary;cycleway=track", withModes(ALL).bicycleSafety(0.85));
    props.setProperties("highway=primary_link;cycleway=track", withModes(ALL).bicycleSafety(0.85));
    props.setProperties(
      "highway=trunk;cycleway=track",
      withModes(BICYCLE_AND_CAR).bicycleSafety(0.95)
    );
    props.setProperties(
      "highway=trunk_link;cycleway=track",
      withModes(BICYCLE_AND_CAR).bicycleSafety(0.85)
    );

    /* cycleway=opposite_track */
    props.setProperties(
      "highway=*;cycleway=opposite_track",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.0, 0.75)
    );
    props.setProperties(
      "highway=service;cycleway=opposite_track",
      withModes(ALL).bicycleSafety(1.1, 0.65)
    );
    props.setProperties(
      "highway=residential;cycleway=opposite_track",
      withModes(ALL).bicycleSafety(0.98, 0.65)
    );
    props.setProperties(
      "highway=residential_link;cycleway=opposite_track",
      withModes(ALL).bicycleSafety(0.98, 0.65)
    );
    props.setProperties(
      "highway=tertiary;cycleway=opposite_track",
      withModes(ALL).bicycleSafety(1, 0.75)
    );
    props.setProperties(
      "highway=tertiary_link;cycleway=opposite_track",
      withModes(ALL).bicycleSafety(1, 0.75)
    );
    props.setProperties(
      "highway=secondary;cycleway=opposite_track",
      withModes(ALL).bicycleSafety(1.5, 0.8)
    );
    props.setProperties(
      "highway=secondary_link;cycleway=opposite_track",
      withModes(ALL).bicycleSafety(1.5, 0.8)
    );
    props.setProperties(
      "highway=primary;cycleway=opposite_track",
      withModes(ALL).bicycleSafety(2.06, 0.85)
    );
    props.setProperties(
      "highway=primary_link;cycleway=opposite_track",
      withModes(ALL).bicycleSafety(2.06, 0.85)
    );
    props.setProperties(
      "highway=trunk;cycleway=opposite_track",
      withModes(BICYCLE_AND_CAR).bicycleSafety(7.47, 0.95)
    );
    props.setProperties(
      "highway=trunk_link;cycleway=opposite_track",
      withModes(BICYCLE_AND_CAR).bicycleSafety(2.06, 0.85)
    );

    /* cycleway=shared_lane a.k.a. bike boulevards or neighborhood greenways */
    props.setProperties(
      "highway=*;cycleway=shared_lane",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(0.77)
    );
    props.setProperties("highway=service;cycleway=shared_lane", withModes(ALL).bicycleSafety(0.73));
    props.setProperties(
      "highway=residential;cycleway=shared_lane",
      withModes(ALL).bicycleSafety(0.77)
    );
    props.setProperties(
      "highway=residential_link;cycleway=shared_lane",
      withModes(ALL).bicycleSafety(0.77)
    );
    props.setProperties(
      "highway=tertiary;cycleway=shared_lane",
      withModes(ALL).bicycleSafety(0.83)
    );
    props.setProperties(
      "highway=tertiary_link;cycleway=shared_lane",
      withModes(ALL).bicycleSafety(0.83)
    );
    props.setProperties(
      "highway=secondary;cycleway=shared_lane",
      withModes(ALL).bicycleSafety(1.25)
    );
    props.setProperties(
      "highway=secondary_link;cycleway=shared_lane",
      withModes(ALL).bicycleSafety(1.25)
    );
    props.setProperties("highway=primary;cycleway=shared_lane", withModes(ALL).bicycleSafety(1.75));
    props.setProperties(
      "highway=primary_link;cycleway=shared_lane",
      withModes(ALL).bicycleSafety(1.75)
    );

    /* cycleway=opposite */
    props.setProperties(
      "highway=*;cycleway=opposite",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1, 1.4)
    );
    props.setProperties("highway=service;cycleway=opposite", withModes(ALL).bicycleSafety(1.1));
    props.setProperties(
      "highway=residential;cycleway=opposite",
      withModes(ALL).bicycleSafety(0.98)
    );
    props.setProperties(
      "highway=residential_link;cycleway=opposite",
      withModes(ALL).bicycleSafety(0.98)
    );
    props.setProperties("highway=tertiary;cycleway=opposite", allWayProperties);
    props.setProperties("highway=tertiary_link;cycleway=opposite", allWayProperties);
    props.setProperties(
      "highway=secondary;cycleway=opposite",
      withModes(ALL).bicycleSafety(1.5, 1.71)
    );
    props.setProperties(
      "highway=secondary_link;cycleway=opposite",
      withModes(ALL).bicycleSafety(1.5, 1.71)
    );
    props.setProperties(
      "highway=primary;cycleway=opposite",
      withModes(ALL).bicycleSafety(2.06, 2.99)
    );
    props.setProperties(
      "highway=primary_link;cycleway=opposite",
      withModes(ALL).bicycleSafety(2.06, 2.99)
    );

    /*
     * path designed for bicycles (should be treated exactly as a cycleway is), this is a multi-use path (MUP)
     */
    props.setProperties(
      "highway=path;bicycle=designated",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(0.60)
    );

    /* special cases for footway, pedestrian and bicycles */
    props.setProperties(
      "highway=footway;bicycle=designated",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(0.75)
    );
    props.setProperties(
      "highway=footway;bicycle=yes;area=yes",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(0.9)
    );
    props.setProperties(
      "highway=pedestrian;bicycle=designated",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(0.75)
    );

    /* sidewalk and crosswalk */
    props.setProperties(
      "footway=sidewalk;highway=footway;bicycle=yes",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(2.5)
    );
    props.setProperties(
      "footway=sidewalk;highway=footway;bicycle=designated",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.1)
    );
    props.setProperties(
      "highway=footway;footway=crossing",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(2.5)
    );
    props.setProperties(
      "highway=footway;footway=crossing;bicycle=designated",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.1)
    );

    /*
     * bicycles on tracks (tracks are defined in OSM as: Roads for agricultural use, gravel roads in the forest etc.; usually unpaved/unsealed but
     * may occasionally apply to paved tracks as well.)
     */
    props.setProperties(
      "highway=track;bicycle=yes",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.18)
    );
    props.setProperties(
      "highway=track;bicycle=designated",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(0.99)
    );
    props.setProperties(
      "highway=track;bicycle=yes;surface=*",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.18)
    );
    props.setProperties(
      "highway=track;bicycle=designated;surface=*",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(0.99)
    );
    /* this is to avoid double counting since tracks are almost of surface type that is penalized */
    props.setProperties(
      "highway=track;surface=*",
      withModes(PEDESTRIAN_AND_BICYCLE).bicycleSafety(1.3)
    );

    /* bicycle=designated, but no bike infrastructure is present */
    props.setProperties("highway=*;bicycle=designated", withModes(ALL).bicycleSafety(0.97));
    props.setProperties("highway=service;bicycle=designated", withModes(ALL).bicycleSafety(0.84));
    props.setProperties(
      "highway=residential;bicycle=designated",
      withModes(ALL).bicycleSafety(0.95)
    );
    props.setProperties(
      "highway=unclassified;bicycle=designated",
      withModes(ALL).bicycleSafety(0.95)
    );
    props.setProperties(
      "highway=residential_link;bicycle=designated",
      withModes(ALL).bicycleSafety(0.95)
    );
    props.setProperties("highway=tertiary;bicycle=designated", withModes(ALL).bicycleSafety(0.97));
    props.setProperties(
      "highway=tertiary_link;bicycle=designated",
      withModes(ALL).bicycleSafety(0.97)
    );
    props.setProperties("highway=secondary;bicycle=designated", withModes(ALL).bicycleSafety(1.46));
    props.setProperties(
      "highway=secondary_link;bicycle=designated",
      withModes(ALL).bicycleSafety(1.46)
    );
    props.setProperties("highway=primary;bicycle=designated", withModes(ALL).bicycleSafety(2));
    props.setProperties("highway=primary_link;bicycle=designated", withModes(ALL).bicycleSafety(2));
    props.setProperties(
      "highway=trunk;bicycle=designated",
      withModes(BICYCLE_AND_CAR).bicycleSafety(7.25)
    );
    props.setProperties(
      "highway=trunk_link;bicycle=designated",
      withModes(BICYCLE_AND_CAR).bicycleSafety(2)
    );
    props.setProperties(
      "highway=motorway;bicycle=designated",
      withModes(BICYCLE_AND_CAR).bicycleSafety(7.76)
    );
    props.setProperties(
      "highway=motorway_link;bicycle=designated",
      withModes(BICYCLE_AND_CAR).bicycleSafety(2)
    );

    // We assume highway/cycleway of a cycle network to be safer (for bicycle network relations, their network is copied to way in postLoad)
    // this uses a OR since you don't want to apply the safety multiplier more than once.
    props.setMixinProperties(
      new LogicalOrSpecifier("lcn=yes", "rcn=yes", "ncn=yes"),
      withModes(ALL).bicycleSafety(0.7)
    );

    /*
     * Automobile speeds in the United States: Based on my (mattwigway) personal experience, primarily in California
     */
    props.setCarSpeed("highway=motorway", 29); // 29 m/s ~= 65 mph
    props.setCarSpeed("highway=motorway_link", 15); // ~= 35 mph
    props.setCarSpeed("highway=trunk", 24.6f); // ~= 55 mph
    props.setCarSpeed("highway=trunk_link", 15); // ~= 35 mph
    props.setCarSpeed("highway=primary", 20); // ~= 45 mph
    props.setCarSpeed("highway=primary_link", 11.2f); // ~= 25 mph
    props.setCarSpeed("highway=secondary", 15); // ~= 35 mph
    props.setCarSpeed("highway=secondary_link", 11.2f); // ~= 25 mph
    props.setCarSpeed("highway=tertiary", 11.2f); // ~= 25 mph
    props.setCarSpeed("highway=tertiary_link", 11.2f); // ~= 25 mph
    props.setCarSpeed("highway=living_street", 2.2f); // ~= 5 mph

    // generally, these will not allow cars at all, but the docs say
    // "For roads used mainly/exclusively for pedestrians . . . which may allow access by
    // motorised vehicles only for very limited periods of the day."
    // http://wiki.openstreetmap.org/wiki/Key:highway
    // This of course makes the street network time-dependent
    props.setCarSpeed("highway=pedestrian", 2.2f); // ~= 5 mph

    props.setCarSpeed("highway=residential", 11.2f); // ~= 25 mph
    props.setCarSpeed("highway=unclassified", 11.2f); // ~= 25 mph
    props.setCarSpeed("highway=service", 6.7f); // ~= 15 mph
    props.setCarSpeed("highway=track", 4.5f); // ~= 10 mph
    props.setCarSpeed("highway=road", 11.2f); // ~= 25 mph

    // default ~= 25 mph
    props.defaultSpeed = 11.2f;

    /* special situations */

    /*
     * cycleway:left/right=lane/track/shared_lane permutations - no longer needed because left/right matching algorithm does this
     */

    /* cycleway:left=lane */
    /* cycleway:right=track */
    /* cycleway:left=track */
    /* cycleway:right=shared_lane */
    /* cycleway:left=shared_lane */
    /* cycleway:right=lane, cycleway:left=track */
    /* cycleway:right=lane, cycleway:left=shared_lane */
    /* cycleway:right=track, cycleway:left=lane */
    /* cycleway:right=track, cycleway:left=shared_lane */
    /* cycleway:right=shared_lane, cycleway:left=lane */
    /* cycleway:right=shared_lane, cycleway:left=track */

    /* surface=* mixins */

    /*
     * The following tags have been removed from surface weights because they are no more of an impedence to bicycling than a paved surface
     * surface=paving_stones surface=fine_gravel (sounds counter-intuitive but see the definition on the OSM Wiki) surface=tartan (this what
     * running tracks are usually made of)
     */

    props.setMixinProperties("surface=unpaved", withModes(ALL).bicycleSafety(1.18));
    props.setMixinProperties("surface=compacted", withModes(ALL).bicycleSafety(1.18));
    props.setMixinProperties("surface=wood", withModes(ALL).bicycleSafety(1.18));

    props.setMixinProperties("surface=cobblestone", withModes(ALL).bicycleSafety(1.3));
    props.setMixinProperties("surface=cobblestone:flattened", withModes(ALL).bicycleSafety(1.3));
    props.setMixinProperties("surface=grass_paver", withModes(ALL).bicycleSafety(1.3));
    props.setMixinProperties("surface=pebblestone", withModes(ALL).bicycleSafety(1.3));
    // Can be slick if wet, but otherwise not unfavorable to bikes
    props.setMixinProperties("surface=metal", withModes(ALL).bicycleSafety(1.3));
    props.setMixinProperties("surface=ground", withModes(ALL).bicycleSafety(1.5));
    props.setMixinProperties("surface=dirt", withModes(ALL).bicycleSafety(1.5));
    props.setMixinProperties("surface=earth", withModes(ALL).bicycleSafety(1.5));
    props.setMixinProperties("surface=grass", withModes(ALL).bicycleSafety(1.5));
    props.setMixinProperties("surface=mud", withModes(ALL).bicycleSafety(1.5));
    props.setMixinProperties("surface=woodchip", withModes(ALL).bicycleSafety(1.5));
    props.setMixinProperties("surface=gravel", withModes(ALL).bicycleSafety(1.5));
    props.setMixinProperties("surface=artifical_turf", withModes(ALL).bicycleSafety(1.5));

    /* sand is deadly for bikes */
    props.setMixinProperties("surface=sand", withModes(ALL).bicycleSafety(100));

    /* Portland-local mixins */

    /*
     * the RLIS/CCGIS:bicycle=designated mixins are coded out as they are no longer neccessary because of of the bicycle=designated block of code
     * above. This switch makes our weighting system less reliant on tags that aren't generally used by the OSM community, and prevents the double
     * counting that was occuring on streets with both bicycle infrastructure and an RLIS:bicycle=designated tag
     */

    /*
     * props.setProperties("RLIS:bicycle=designated", StreetTraversalPermission.ALL, 0.97, 0.97, true);
     */
    props.setMixinProperties("RLIS:bicycle=caution_area", withModes(ALL).bicycleSafety(1.45));
    props.setMixinProperties(
      "RLIS:bicycle:right=caution_area",
      withModes(ALL).bicycleSafety(1.45, 1)
    );
    props.setMixinProperties(
      "RLIS:bicycle:left=caution_area",
      withModes(ALL).bicycleSafety(1, 1.45)
    );
    /*
     * props.setProperties("CCGIS:bicycle=designated", StreetTraversalPermission.ALL, 0.97, 0.97, true);
     */
    props.setMixinProperties("CCGIS:bicycle=caution_area", withModes(ALL).bicycleSafety(1.45));
    props.setMixinProperties(
      "CCGIS:bicycle:right=caution_area",
      withModes(ALL).bicycleSafety(1.45, 1)
    );
    props.setMixinProperties(
      "CCGIS:bicycle:left=caution_area",
      withModes(ALL).bicycleSafety(1, 1.45)
    );

    populateNotesAndNames(props);

    // slope overrides
    props.setSlopeOverride(new BestMatchSpecifier("bridge=*"), true);
    props.setSlopeOverride(new BestMatchSpecifier("embankment=*"), true);
    props.setSlopeOverride(new BestMatchSpecifier("tunnel=*"), true);
  }

  public void populateNotesAndNames(WayPropertySet props) {
    /* and the notes */
    // TODO: The curly brackets in the string below mean that the CreativeNamer should substitute in OSM tag values.
    // However they are not taken into account when passed to the translation function.
    // props.createNotes("wheelchair:description=*", "{wheelchair:description}", StreetNotesService.WHEELCHAIR_MATCHER);
    // TODO: The two entries below produce lots of spurious notes (because of OSM mapper comments)
    // props.createNotes("note=*", "{note}", StreetNotesService.ALWAYS_MATCHER);
    // props.createNotes("notes=*", "{notes}", StreetNotesService.ALWAYS_MATCHER);
    props.createNotes(
      "RLIS:bicycle=caution_area",
      "note.caution",
      StreetNotesService.BICYCLE_MATCHER
    );
    props.createNotes(
      "CCGIS:bicycle=caution_area",
      "note.caution",
      StreetNotesService.BICYCLE_MATCHER
    );
    // TODO: Maybe we should apply the following notes only for car/bike
    props.createNotes("surface=unpaved", "note.unpaved_surface", StreetNotesService.ALWAYS_MATCHER);
    props.createNotes(
      "surface=compacted",
      "note.unpaved_surface",
      StreetNotesService.ALWAYS_MATCHER
    );
    props.createNotes("surface=ground", "note.unpaved_surface", StreetNotesService.ALWAYS_MATCHER);
    props.createNotes("surface=dirt", "note.unpaved_surface", StreetNotesService.ALWAYS_MATCHER);
    props.createNotes("surface=earth", "note.unpaved_surface", StreetNotesService.ALWAYS_MATCHER);
    props.createNotes("surface=grass", "note.unpaved_surface", StreetNotesService.ALWAYS_MATCHER);
    props.createNotes("surface=mud", "note.muddy_surface", StreetNotesService.ALWAYS_MATCHER);
    props.createNotes("toll=yes", "note.toll", StreetNotesService.DRIVING_MATCHER);
    props.createNotes("toll:motorcar=yes", "note.toll", StreetNotesService.DRIVING_MATCHER);

    /* and some names */
    // Basics
    props.createNames("highway=cycleway", "name.bike_path");
    props.createNames("cycleway=track", "name.bike_path");
    props.createNames("highway=pedestrian", "name.pedestrian_path");
    props.createNames("highway=pedestrian;area=yes", "name.pedestrian_area");
    props.createNames("highway=path", "name.path");
    props.createNames("highway=footway", "name.pedestrian_path");
    props.createNames("highway=bridleway", "name.bridleway");
    props.createNames("highway=footway;bicycle=no", "name.pedestrian_path");

    // Platforms
    props.createNames("otp:route_ref=*", "name.otp_route_ref");
    props.createNames("highway=platform;ref=*", "name.platform_ref");
    props.createNames("railway=platform;ref=*", "name.platform_ref");
    props.createNames("railway=platform;highway=footway;footway=sidewalk", "name.platform");
    props.createNames("railway=platform;highway=path;path=sidewalk", "name.platform");
    props.createNames("railway=platform;highway=pedestrian", "name.platform");
    props.createNames("railway=platform;highway=path", "name.platform");
    props.createNames("railway=platform;highway=footway", "name.platform");
    props.createNames("highway=platform", "name.platform");
    props.createNames("railway=platform", "name.platform");
    props.createNames("railway=platform;highway=footway;bicycle=no", "name.platform");

    // Bridges/Tunnels
    props.createNames("highway=pedestrian;bridge=*", "name.footbridge");
    props.createNames("highway=path;bridge=*", "name.footbridge");
    props.createNames("highway=footway;bridge=*", "name.footbridge");

    props.createNames("highway=pedestrian;tunnel=*", "name.underpass");
    props.createNames("highway=path;tunnel=*", "name.underpass");
    props.createNames("highway=footway;tunnel=*", "name.underpass");

    // Basic Mappings
    props.createNames("highway=motorway", "name.road");
    props.createNames("highway=motorway_link", "name.ramp");
    props.createNames("highway=trunk", "name.road");
    props.createNames("highway=trunk_link", "name.ramp");

    props.createNames("highway=primary", "name.road");
    props.createNames("highway=primary_link", "name.link");
    props.createNames("highway=secondary", "name.road");
    props.createNames("highway=secondary_link", "name.link");
    props.createNames("highway=tertiary", "name.road");
    props.createNames("highway=tertiary_link", "name.link");
    props.createNames("highway=unclassified", "name.road");
    props.createNames("highway=residential", "name.road");
    props.createNames("highway=living_street", "name.road");
    props.createNames("highway=road", "name.road");
    props.createNames("highway=service", "name.service_road");
    props.createNames("highway=service;service=alley", "name.alley");
    props.createNames("highway=service;service=parking_aisle", "name.parking_aisle");
    props.createNames("highway=byway", "name.byway");
    props.createNames("highway=track", "name.track");

    props.createNames("highway=footway;footway=sidewalk", "name.sidewalk");
    props.createNames("highway=path;path=sidewalk", "name.sidewalk");

    props.createNames("highway=steps", "name.steps");

    props.createNames("amenity=bicycle_parking;name=*", "name.bicycle_parking_name");
    props.createNames("amenity=bicycle_parking", "name.bicycle_parking");

    props.createNames("amenity=parking;name=*", "name.park_and_ride_name");
    props.createNames("amenity=parking", "name.park_and_ride_station");
  }
}
